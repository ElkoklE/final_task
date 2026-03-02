# Defense Runbook (Step-by-step)

## 0. Start stack
```bash
docker compose up --build -d
```

Check containers:
```bash
docker compose ps
```

## 1. Trigger data collection
```bash
curl -X POST "http://localhost:8080/api/v1/collect?date=2025-11-11"
sleep 3
curl "http://localhost:8080/api/v1/answer?date=2025-11-11&sortBy=rateToRub&direction=desc"
```

## 2. Actuator evidence
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/info
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
curl http://localhost:8080/actuator/threaddump > threaddump.json
curl http://localhost:8080/actuator/heapdump > heapdump.hprof
curl http://localhost:8080/actuator/prometheus | head -n 50
```

Take screenshots:
1. `/actuator/health`
2. `/actuator/info`
3. `/actuator/metrics/http.server.requests`

## 3. Throughput and latency test
If k6 is installed:
```bash
k6 run scripts/k6-answer-load.js
```

Take screenshots:
1. k6 summary output (RPS, p95, errors)
2. Grafana dashboard panels (throughput + latency)

## 4. Prometheus and Grafana
Open:
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (`admin/admin`)

Prometheus queries:
- `rate(http_server_requests_seconds_count{uri=~"/api/v1/.*"}[1m])`
- `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri=~"/api/v1/.*"}[1m])) by (le, uri))`
- `currency_parser_parse_success_total`
- `currency_parser_parse_error_total`
- `currency_parser_records_persisted_total`
- `currency_parser_queue_size`
- `jvm_gc_pause_seconds_count`
- `jvm_gc_pause_seconds_max`

Take screenshots:
1. Prometheus graph with HTTP metric
2. Grafana dashboard full view

## 5. Tracing in Jaeger
Open `http://localhost:16686`, service `currency-mt-parser`.

Expected spans:
- `currency.api.collect`
- `currency.api.answer`
- `currency.api.answer.by_date`
- `currency.parser.collection`
- `currency.parser.provider`
- `currency.parser.persist.batch`

Take screenshots:
1. Trace list
2. One full trace with span timings

## 6. JMH benchmarks
```bash
./gradlew jmh
```

Take screenshot:
1. JMH console result with three methods

## 7. JFR / GC analysis
Run with GC log and JFR (outside docker, local run):
```bash
./gradlew bootRun -Dorg.gradle.jvmargs='-Xms512m -Xmx512m -Xlog:gc*:file=gc.log:time,uptime,level,tags -XX:StartFlightRecording=filename=recording.jfr,duration=120s,settings=profile'
```

Analyze:
- `recording.jfr` in JDK Mission Control / VisualVM
- `heapdump.hprof` in VisualVM
- `gc.log` for pause frequency and max pause

Take screenshots:
1. CPU hotspot view
2. Allocation view
3. GC pauses
4. Heap top objects

## 8. Stop stack
```bash
docker compose down
```
