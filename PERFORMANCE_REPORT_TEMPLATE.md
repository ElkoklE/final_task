# Performance Report Template

## 1. Environment
- Java: 17
- Spring Boot: 3.3.5
- Run profile: local/docker
- Test date: <fill date>
- Commit: <fill hash>

## 2. Actuator + Micrometer + Prometheus
- `/actuator/health`: <status>
- `/actuator/info`: <key fields>
- `/actuator/metrics`: checked
- `/actuator/prometheus`: checked
- `/actuator/threaddump`: collected and analyzed
- `/actuator/heapdump`: collected and analyzed

Custom metrics used:
- `currency.parser.parse.success.total`
- `currency.parser.parse.error.total`
- `currency.parser.records.persisted.total`
- `currency.parser.collection.duration`
- `currency.parser.provider.duration`
- `currency.parser.queue.persist.duration`
- `currency.parser.queue.size`

## 3. Throughput and Latency (real endpoint load)
- Tool: k6 (`scripts/k6-answer-load.js`)
- Endpoint: `GET /api/v1/answer`
- Test params: 20 VUs, 60s
- Throughput (RPS): <fill>
- Latency p50/p95/p99: <fill>
- Error rate: <fill>

## 4. JFR / VisualVM Profiling
- CPU hotspots:
  - <method + %>
- Allocation hotspots:
  - <class + bytes>
- Thread dump findings:
  - <locks/waits>
- Heap dump findings:
  - <top retainers>

## 5. GC Analysis
- JVM flags used:
  - `-Xms512m -Xmx512m -Xlog:gc*:file=gc.log:time,uptime,level,tags`
- GC frequency: <fill>
- Max pause: <fill>
- Most allocated objects: <fill>

## 6. Performance Degradation Fixes
1. Repeated DB writes reduced with batched persistence (`saveAll` + transaction).
2. Indexes added for critical query path (`currency_code`, `rate_date`, `collected_at`).
3. Reduced lock contention (status map without explicit lock).
4. Batching reduced frequent small allocations during persistence.

## 7. JMH Benchmarks
Run command:
```bash
./gradlew jmh
```

Results (fill from your run):
- `aggregateWithFor`: <time/op>
- `aggregateWithStream`: <time/op>
- `aggregateWithParallelStream`: <time/op>

Conclusion on algorithmic complexity and structure choice:
- All strategies are O(n), but constant factors differ.
- For moderate collections, `for` often wins by lower overhead.
- `parallelStream` is justified only when collection size is large and CPU cores are available.
- `ConcurrentHashMap` in parallel aggregation removes global lock contention vs synchronized map approach.

## 8. Distributed Tracing (OpenTelemetry + Jaeger)
- OTel exporter endpoint configured.
- Jaeger UI: `http://localhost:16686`
- Trace examples captured:
  - `currency.api.collect`
  - `currency.api.answer`
  - `currency.parser.collection`
- Stage durations interpreted: <fill>

## 9. Bounded Contexts
Proposed bounded contexts:
1. `rate-ingestion` (providers, parser, collection scheduling)
2. `rate-storage` (persistence, indexing, transactional consistency)
3. `rate-query` (API read models, sorting/filtering)
4. `observability` (metrics, tracing, profiling artifacts)

## 10. Before/After Comparison
- Throughput before: <fill>
- Throughput after: <fill>
- P95 latency before: <fill>
- P95 latency after: <fill>
- GC pauses before/after: <fill>
- Final conclusion: <fill>
