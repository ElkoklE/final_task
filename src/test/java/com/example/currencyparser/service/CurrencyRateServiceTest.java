package com.example.currencyparser.service;

import com.example.currencyparser.client.ExternalRateProvider;
import com.example.currencyparser.dto.CurrencyRateResponse;
import com.example.currencyparser.model.CurrencyRate;
import com.example.currencyparser.repository.CurrencyRateRepository;
import com.example.currencyparser.service.CurrencyRatePersistenceService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyRateServiceTest {

    @Mock
    private CurrencyRateRepository repository;

    @Mock
    private CurrencyRatePersistenceService persistenceService;

    @Test
    void shouldSortUsingParallelStreamByRateToRubDesc() {
        CurrencyRate usd = rate("USD", "100.10");
        CurrencyRate eur = rate("EUR", "108.99");
        CurrencyRate cny = rate("CNY", "12.00");

        when(repository.findByRateDate(LocalDate.of(2025, 11, 11)))
                .thenReturn(List.of(usd, eur, cny));

        CurrencyRateService service = new CurrencyRateService(
                List.<ExternalRateProvider>of(),
                repository,
                persistenceService,
                Executors.newFixedThreadPool(2),
                Executors.newSingleThreadScheduledExecutor(),
                new SimpleMeterRegistry(),
                ObservationRegistry.NOOP
        );

        List<CurrencyRateResponse> result = service.getRatesForDateSorted(
                LocalDate.of(2025, 11, 11),
                "rateToRub",
                "desc"
        );

        assertEquals("EUR", result.get(0).currencyCode());
        assertEquals("USD", result.get(1).currencyCode());
        assertEquals("CNY", result.get(2).currencyCode());
    }

    private CurrencyRate rate(String code, String value) {
        CurrencyRate currencyRate = new CurrencyRate();
        currencyRate.setCurrencyCode(code);
        currencyRate.setRateToRub(new BigDecimal(value));
        currencyRate.setRateToUsd(BigDecimal.ONE);
        currencyRate.setDayChangePercent(BigDecimal.ZERO);
        currencyRate.setRateDate(LocalDate.of(2025, 11, 11));
        currencyRate.setCollectedAt(LocalDateTime.now());
        currencyRate.setSource("TEST");
        return currencyRate;
    }
}
