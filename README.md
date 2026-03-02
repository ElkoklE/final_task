# Currency MT Parser (Spring Boot, Java)

Итоговый проект по дисциплине «Управление производительностью приложений» на базе многопоточного парсера курсов валют.

## Что реализовано
- Многопоточный сбор курсов (`ExecutorService`, `ScheduledExecutorService`, `BlockingQueue`)
- REST API для запуска сбора и выдачи результатов
- H2 + Spring Data JPA
- Корректные транзакции для пакетной записи (декларативные `@Transactional` + `saveAll`)
- Индексы в таблице `currency_rates` под критичные запросы
- Уменьшена деградация из-за синхронизации: отмена «медленных» provider-задач сразу после первого успешного результата
- Spring Boot Actuator
- Micrometer + Prometheus + Grafana
- Кастомные метрики парсинга/сохранения
- OpenTelemetry tracing + Jaeger
- JMH микро-бенчмарки (`for` vs `stream` vs `parallelStream`)

## Быстрый запуск

### Вариант 1: локально
```bash
./gradlew bootRun
```

### Вариант 2: полный observability-стек
```bash
docker compose up --build
```

Доступы:
- App: `http://localhost:8080`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (`admin/admin`)
- Jaeger: `http://localhost:16686`

## API
- `POST /api/v1/collect?date=2025-11-11`
- `GET /api/v1/answer?date=2025-11-11&sortBy=rateToRub&direction=desc`
- `GET /api/v1/answer/by-date?date=2025-11-11&currency=USD`
- `GET /api/v1/status`

## Actuator endpoints (критерии проверки)
- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`
- `GET /actuator/threaddump`
- `GET /actuator/heapdump`
- HTTP метрики: `http.server.requests`

Примеры проверки:
```bash
curl http://localhost:8080/actuator/threaddump
curl http://localhost:8080/actuator/metrics/http.server.requests
```

## Кастомные метрики Micrometer
- `currency.parser.parse.success.total`
- `currency.parser.parse.error.total`
- `currency.parser.records.persisted.total`
- `currency.parser.queue.drop.total`
- `currency.parser.queue.size`
- `currency.parser.collection.duration`
- `currency.parser.provider.duration`
- `currency.parser.queue.persist.duration`

## Нагрузочное тестирование (throughput/latency)
Пример реального теста endpoint `GET /api/v1/answer`:
```bash
k6 run scripts/k6-answer-load.js
```

Графики смотреть в Grafana (дашборд `Currency MT Parser Performance`).

## JFR / VisualVM / GC

### JFR
```bash
./gradlew bootRun --args='--spring.profiles.active=default' \
  -Dorg.gradle.jvmargs='-XX:StartFlightRecording=filename=recording.jfr,duration=120s,settings=profile'
```

### GC лог
```bash
./gradlew bootRun -Dorg.gradle.jvmargs='-Xms512m -Xmx512m -Xlog:gc*:file=gc.log:time,uptime,level,tags'
```

Что анализировать:
- hotspot-методы (CPU)
- аллокации и top retainers
- частоту/паузы GC
- thread dump: WAITING/BLOCKED и lock contention
- heap dump: крупные доминирующие объекты

Дополнительно по GC из Prometheus:
- `jvm_gc_pause_seconds_count`
- `jvm_gc_pause_seconds_max`
- `jvm_memory_used_bytes`

## JMH
```bash
./gradlew jmh
```

Benchmark: `RateAggregationBenchmark`
- `aggregateWithFor`
- `aggregateWithStream`
- `aggregateWithParallelStream`

Пример фактического запуска (2 марта 2026):
- `aggregateWithFor`: `423.290 us/op`
- `aggregateWithParallelStream`: `456.025 us/op`
- `aggregateWithStream`: `565.798 us/op`

## Архитектурные выводы
- Критичные участки обработки коллекций имеют O(n), выбор стратегии определяется константными издержками.
- `ConcurrentHashMap` в параллельной агрегации предпочтительнее глобальной синхронизации.
- Рекомендуемые bounded contexts:
  - `rate-ingestion`
  - `rate-storage`
  - `rate-query`
  - `observability`

Трассинг этапов в Jaeger:
- `currency.api.collect`
- `currency.api.answer`
- `currency.api.answer.by_date`
- `currency.parser.collection`
- `currency.parser.provider`
- `currency.parser.persist.batch`

## Тесты
```bash
./gradlew test
```

## Шаблон отчета для сдачи
См. `PERFORMANCE_REPORT_TEMPLATE.md`.
