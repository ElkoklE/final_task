package com.example.currencyparser.client;

import java.math.BigDecimal;

public record ParsedRate(
        String currencyCode,
        BigDecimal rateToRub,
        BigDecimal dayChangePercent
) {
}
