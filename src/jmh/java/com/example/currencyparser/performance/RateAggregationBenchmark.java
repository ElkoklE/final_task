package com.example.currencyparser.performance;

import com.example.currencyparser.client.ParsedRate;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class RateAggregationBenchmark {

    private List<ParsedRate> benchmarkData;

    @Setup(Level.Trial)
    public void setup() {
        String[] currencies = {"USD", "EUR", "CNY", "GBP", "JPY", "CHF", "CAD", "NOK", "SEK", "TRY"};
        benchmarkData = new ArrayList<>(50_000);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 50_000; i++) {
            String code = currencies[random.nextInt(currencies.length)] + (i % 100);
            BigDecimal rate = BigDecimal.valueOf(50 + random.nextDouble(120)).setScale(6, RoundingMode.HALF_UP);
            BigDecimal change = BigDecimal.valueOf(random.nextDouble(-3, 3)).setScale(4, RoundingMode.HALF_UP);
            benchmarkData.add(new ParsedRate(code, rate, change));
        }
    }

    @Benchmark
    public int aggregateWithFor() {
        return RateAggregationStrategies.aggregateWithFor(benchmarkData).size();
    }

    @Benchmark
    public int aggregateWithStream() {
        return RateAggregationStrategies.aggregateWithStream(benchmarkData).size();
    }

    @Benchmark
    public int aggregateWithParallelStream() {
        return RateAggregationStrategies.aggregateWithParallelStream(benchmarkData).size();
    }
}
