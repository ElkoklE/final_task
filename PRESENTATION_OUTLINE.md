# Presentation Outline (10-12 slides)

## Slide 1. Title
- Project: Currency MT Parser
- Course: Application Performance Management
- Goal: diagnose and improve performance

## Slide 2. System and workload
- Architecture overview (ingestion, storage, query, observability)
- Real endpoint under test: `GET /api/v1/answer`
- Why this endpoint is critical

## Slide 3. Instrumentation stack
- Actuator + Micrometer + Prometheus + Grafana
- OpenTelemetry + Jaeger
- VisualVM/JFR + JMH

## Slide 4. Baseline metrics
- Throughput, p95 latency, error rate before optimization
- Initial bottlenecks (CPU/GC/DB or lock contention)

## Slide 5. Custom metrics implementation
- Parse success/error counters
- Persisted records counter
- Stage timers and queue gauge
- Screenshot from Grafana panels

## Slide 6. Profiling results (JFR/VisualVM)
- Slow methods (CPU hotspots)
- Allocation hotspots
- Thread dump interpretation
- Heap dump top objects

## Slide 7. GC analysis
- GC frequency and pauses
- Memory pressure source
- Impact on latency

## Slide 8. Performance fixes
- Batched transactional persistence (`@Transactional` + `saveAll`)
- Indexes for query path
- Reduced lock contention / better concurrent structures
- Any N+1 prevention and query optimization

## Slide 9. JMH comparison
- `for` vs `stream` vs `parallelStream`
- Numeric results table
- Complexity rationale: O(n) with different constants

## Slide 10. Distributed tracing
- Jaeger trace screenshot
- Stage-by-stage timings
- Where time is spent in request lifecycle

## Slide 11. Before/After comparison
- Throughput delta
- Latency p95 delta
- Error rate delta
- GC pause delta

## Slide 12. Conclusions
- What improved and why
- Tradeoffs and limits
- Bounded contexts for microservice decomposition
