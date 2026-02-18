package com.example.currencyparser.service;

import com.example.currencyparser.client.ExternalRateProvider;
import com.example.currencyparser.client.ParsedRate;
import com.example.currencyparser.dto.CurrencyRateResponse;
import com.example.currencyparser.model.CurrencyRate;
import com.example.currencyparser.repository.CurrencyRateRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class CurrencyRateService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyRateService.class);

    private static final Set<String> TARGET_CURRENCIES = Set.of("USD", "EUR", "CNY", "GBP", "JPY");

    private final List<ExternalRateProvider> providers;
    private final CurrencyRateRepository repository;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final BlockingQueue<QueuedRate> persistenceQueue = new LinkedBlockingQueue<>(1000);

    private final ReentrantLock statusLock = new ReentrantLock();
    private final ConcurrentMap<String, String> lastCollectionStatus = new ConcurrentHashMap<>();

    private volatile boolean running;

    @Value("${app.collection.interval-seconds:300}")
    private long intervalSeconds;

    public CurrencyRateService(
            List<ExternalRateProvider> providers,
            CurrencyRateRepository repository,
            ExecutorService currencyExecutorService,
            ScheduledExecutorService currencyScheduledExecutorService
    ) {
        this.providers = providers;
        this.repository = repository;
        this.executorService = currencyExecutorService;
        this.scheduledExecutorService = currencyScheduledExecutorService;
    }

    @PostConstruct
    public void startBackgroundWorkers() {
        running = true;

        Thread consumerThread = new Thread(new QueueConsumer(), "queue-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();

        Thread daemonLogger = new Thread(new QueueLogger(), "queue-logger");
        daemonLogger.setDaemon(true);
        daemonLogger.start();

        scheduledExecutorService.scheduleWithFixedDelay(
                () -> triggerCollection(LocalDate.now()),
                1,
                intervalSeconds,
                TimeUnit.SECONDS
        );

        triggerCollection(LocalDate.now());
    }

    @PreDestroy
    public void stopBackgroundWorkers() {
        running = false;
    }

    public void triggerCollection(LocalDate date) {
        executorService.submit(() -> collectAndQueue(date));
    }

    public List<CurrencyRateResponse> getRatesForDateSorted(LocalDate date, String sortBy, String direction) {
        Comparator<CurrencyRate> comparator = comparatorFor(sortBy);
        if ("desc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }

        return repository.findByRateDate(date)
                .parallelStream()
                .sorted(comparator)
                .map(CurrencyRateResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<CurrencyRateResponse> getByDateAndCurrency(LocalDate date, String currency) {
        return repository.findTopByRateDateAndCurrencyCodeIgnoreCaseOrderByCollectedAtDesc(date, currency)
                .map(CurrencyRateResponse::fromEntity);
    }

    public Map<String, String> getCollectionStatus() {
        statusLock.lock();
        try {
            return Map.copyOf(lastCollectionStatus);
        } finally {
            statusLock.unlock();
        }
    }

    private void collectAndQueue(LocalDate date) {
        CompletionService<ProviderResult> completionService = new ExecutorCompletionService<>(executorService);
        CountDownLatch latch = new CountDownLatch(providers.size());

        List<Future<ProviderResult>> futures = new ArrayList<>();
        for (ExternalRateProvider provider : providers) {
            futures.add(completionService.submit(() -> {
                try {
                    return new ProviderResult(provider.providerName(), provider.loadRatesForDate(date));
                } finally {
                    latch.countDown();
                }
            }));
        }

        ProviderResult firstSuccess = null;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < providers.size(); i++) {
            try {
                Future<ProviderResult> completed = completionService.take();
                ProviderResult result = completed.get();
                if (result.parsedRates() != null && !result.parsedRates().isEmpty()) {
                    firstSuccess = result;
                    break;
                }
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        }

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errors.add("Interrupted while waiting provider tasks");
        }

        for (Future<ProviderResult> future : futures) {
            future.cancel(true);
        }

        if (firstSuccess == null) {
            ProviderResult fallback = fallbackResult(date, errors);
            queueParsedRates(date, fallback);
            updateStatus("DEGRADED", "Fallback rates generated for " + date + " after provider errors");
            log.warn("Could not collect rates for {}. Errors: {}. Using fallback data.", date, errors);
            return;
        }

        queueParsedRates(date, firstSuccess);
        updateStatus("OK", "Collected by " + firstSuccess.providerName() + " for " + date);
    }

    private void queueParsedRates(LocalDate date, ProviderResult providerResult) {
        ParsedRate usdRate = providerResult.parsedRates().get("USD");
        if (usdRate == null) {
            updateStatus("FAILED", "USD is missing in provider response");
            return;
        }

        for (String target : TARGET_CURRENCIES) {
            ParsedRate parsed = providerResult.parsedRates().get(target);
            if (parsed == null) {
                continue;
            }

            BigDecimal rateToUsd = parsed.rateToRub()
                    .divide(usdRate.rateToRub(), 6, RoundingMode.HALF_UP);

            QueuedRate queuedRate = new QueuedRate(
                    target,
                    parsed.rateToRub(),
                    rateToUsd,
                    parsed.dayChangePercent().setScale(4, RoundingMode.HALF_UP),
                    date,
                    LocalDateTime.now(),
                    providerResult.providerName()
            );

            boolean offered = persistenceQueue.offer(queuedRate);
            if (!offered) {
                log.warn("Queue is full. Skipping {} for {}", target, date);
            }
        }
    }

    private ProviderResult fallbackResult(LocalDate date, List<String> errors) {
        long seed = date.toEpochDay();
        BigDecimal usd = BigDecimal.valueOf(87 + (seed % 7)).setScale(6, RoundingMode.HALF_UP);
        BigDecimal eur = usd.multiply(BigDecimal.valueOf(1.07)).setScale(6, RoundingMode.HALF_UP);
        BigDecimal cny = usd.multiply(BigDecimal.valueOf(0.138)).setScale(6, RoundingMode.HALF_UP);
        BigDecimal gbp = usd.multiply(BigDecimal.valueOf(1.25)).setScale(6, RoundingMode.HALF_UP);
        BigDecimal jpy = usd.multiply(BigDecimal.valueOf(0.61)).setScale(6, RoundingMode.HALF_UP);

        BigDecimal deltaBase = BigDecimal.valueOf((seed % 9) - 4).divide(BigDecimal.TEN, 4, RoundingMode.HALF_UP);

        Map<String, ParsedRate> map = Map.of(
                "USD", new ParsedRate("USD", usd, deltaBase),
                "EUR", new ParsedRate("EUR", eur, deltaBase.add(BigDecimal.valueOf(0.08))),
                "CNY", new ParsedRate("CNY", cny, deltaBase.subtract(BigDecimal.valueOf(0.05))),
                "GBP", new ParsedRate("GBP", gbp, deltaBase.add(BigDecimal.valueOf(0.12))),
                "JPY", new ParsedRate("JPY", jpy, deltaBase.subtract(BigDecimal.valueOf(0.09)))
        );

        log.warn("Fallback data generated for {} due to provider errors: {}", date, errors);
        return new ProviderResult("FALLBACK_LOCAL", map);
    }

    private void updateStatus(String status, String message) {
        statusLock.lock();
        try {
            lastCollectionStatus.put("status", status);
            lastCollectionStatus.put("message", message);
            lastCollectionStatus.put("updatedAt", LocalDateTime.now().toString());
        } finally {
            statusLock.unlock();
        }
    }

    private Comparator<CurrencyRate> comparatorFor(String sortBy) {
        return switch (sortBy) {
            case "rateToRub" -> Comparator.comparing(CurrencyRate::getRateToRub);
            case "rateToUsd" -> Comparator.comparing(CurrencyRate::getRateToUsd);
            case "dayChangePercent" -> Comparator.comparing(CurrencyRate::getDayChangePercent);
            case "collectedAt" -> Comparator.comparing(CurrencyRate::getCollectedAt);
            default -> Comparator.comparing(CurrencyRate::getCurrencyCode);
        };
    }

    private record ProviderResult(String providerName, Map<String, ParsedRate> parsedRates) {
    }

    private record QueuedRate(
            String currencyCode,
            BigDecimal rateToRub,
            BigDecimal rateToUsd,
            BigDecimal dayChangePercent,
            LocalDate rateDate,
            LocalDateTime collectedAt,
            String source
    ) {
    }

    private class QueueConsumer implements Runnable {
        @Override
        public void run() {
            while (running) {
                try {
                    QueuedRate queuedRate = persistenceQueue.poll(1, TimeUnit.SECONDS);
                    if (queuedRate == null) {
                        continue;
                    }

                    CurrencyRate entity = new CurrencyRate();
                    entity.setCurrencyCode(queuedRate.currencyCode());
                    entity.setRateToRub(queuedRate.rateToRub());
                    entity.setRateToUsd(queuedRate.rateToUsd());
                    entity.setDayChangePercent(queuedRate.dayChangePercent());
                    entity.setRateDate(queuedRate.rateDate());
                    entity.setCollectedAt(queuedRate.collectedAt());
                    entity.setSource(queuedRate.source());
                    repository.save(entity);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    log.error("Failed to persist queued rate", e);
                }
            }
        }
    }

    private class QueueLogger implements Runnable {
        @Override
        public void run() {
            while (running) {
                log.info("Queue size: {}", persistenceQueue.size());
                try {
                    Thread.sleep(20_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
