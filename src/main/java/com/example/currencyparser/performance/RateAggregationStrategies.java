package com.example.currencyparser.performance;

import com.example.currencyparser.client.ParsedRate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class RateAggregationStrategies {

    private RateAggregationStrategies() {
    }

    public static Map<String, BigDecimal> aggregateWithFor(List<ParsedRate> rates) {
        Map<String, BigDecimal> result = new HashMap<>(rates.size());
        for (ParsedRate rate : rates) {
            result.put(rate.currencyCode(), rate.rateToRub());
        }
        return result;
    }

    public static Map<String, BigDecimal> aggregateWithStream(List<ParsedRate> rates) {
        return rates.stream().collect(Collectors.toMap(
                ParsedRate::currencyCode,
                ParsedRate::rateToRub,
                (left, right) -> right,
                HashMap::new
        ));
    }

    public static Map<String, BigDecimal> aggregateWithParallelStream(List<ParsedRate> rates) {
        return rates.parallelStream().collect(Collectors.toConcurrentMap(
                ParsedRate::currencyCode,
                ParsedRate::rateToRub,
                (left, right) -> right,
                ConcurrentHashMap::new
        ));
    }
}
