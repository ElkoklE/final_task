# Сценарий Защиты (Пошагово)

## 0. Запуск стека
```bash
docker compose up --build -d
```

Проверка контейнеров:
```bash
docker compose ps
```

## 1. Запуск сбора данных
```bash
curl -X POST "http://localhost:8080/api/v1/collect?date=2025-11-11"
sleep 3
curl "http://localhost:8080/api/v1/answer?date=2025-11-11&sortBy=rateToRub&direction=desc"
```

## 2. Демонстрация Actuator
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/info
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
curl http://localhost:8080/actuator/threaddump > threaddump.json
curl http://localhost:8080/actuator/heapdump > heapdump.hprof
curl http://localhost:8080/actuator/prometheus | head -n 50
```

Скриншоты для отчета:
1. `/actuator/health`
2. `/actuator/info`
3. `/actuator/metrics/http.server.requests`

## 3. Тест пропускной способности и задержки
Если установлен `k6`:
```bash
k6 run scripts/k6-answer-load.js
```

Скриншоты для отчета:
1. Итог `k6` (RPS, p95, доля ошибок)
2. Панели Grafana (throughput и latency)

## 4. Prometheus и Grafana
Открыть:
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (`admin/admin`)

Запросы в Prometheus:
- `rate(http_server_requests_seconds_count{uri=~"/api/v1/.*"}[1m])`
- `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri=~"/api/v1/.*"}[1m])) by (le, uri))`
- `currency_parser_parse_success_total`
- `currency_parser_parse_error_total`
- `currency_parser_records_persisted_total`
- `currency_parser_queue_size`
- `jvm_gc_pause_seconds_count`
- `jvm_gc_pause_seconds_max`

Скриншоты для отчета:
1. График HTTP-метрик в Prometheus
2. Полный дашборд в Grafana

## 5. Трейсинг в Jaeger
Открыть `http://localhost:16686`, выбрать сервис `currency-mt-parser`.

Ожидаемые спаны:
- `currency.api.collect`
- `currency.api.answer`
- `currency.api.answer.by_date`
- `currency.parser.collection`
- `currency.parser.provider`
- `currency.parser.persist.batch`

Скриншоты для отчета:
1. Список трейсов
2. Один полный трейс с длительностями спанов

## 6. JMH-бенчмарки
```bash
./gradlew jmh
```

Скриншот для отчета:
1. Консольный вывод JMH с тремя методами

## 7. Анализ JFR / GC
Локальный запуск (вне Docker) с GC-логом и JFR:
```bash
./gradlew bootRun -Dorg.gradle.jvmargs='-Xms512m -Xmx512m -Xlog:gc*:file=gc.log:time,uptime,level,tags -XX:StartFlightRecording=filename=recording.jfr,duration=120s,settings=profile'
```

Что анализировать:
- `recording.jfr` в JDK Mission Control / VisualVM
- `heapdump.hprof` в VisualVM
- `gc.log`: частота GC и максимальные паузы

Скриншоты для отчета:
1. CPU hotspots
2. Allocation hotspots
3. Паузы GC
4. Топ объектов в heap

## 8. Остановка стека
```bash
docker compose down
```
