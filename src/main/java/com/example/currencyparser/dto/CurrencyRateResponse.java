package com.example.currencyparser.dto;

import com.example.currencyparser.model.CurrencyRate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CurrencyRateResponse(
        String currencyCode,
        BigDecimal rateToRub,
        BigDecimal rateToUsd,
        BigDecimal dayChangePercent,
        LocalDate rateDate,
        LocalDateTime collectedAt,
        String source
) {
    public static CurrencyRateResponse fromEntity(CurrencyRate entity) {
        return new CurrencyRateResponse(
                entity.getCurrencyCode(),
                entity.getRateToRub(),
                entity.getRateToUsd(),
                entity.getDayChangePercent(),
                entity.getRateDate(),
                entity.getCollectedAt(),
                entity.getSource()
        );
    }
}
