package com.example.currencyparser.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "currency_rates", indexes = {
        @Index(name = "idx_currency_date", columnList = "currencyCode, rateDate")
})
public class CurrencyRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 8)
    private String currencyCode;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rateToRub;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rateToUsd;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal dayChangePercent;

    @Column(nullable = false)
    private LocalDate rateDate;

    @Column(nullable = false)
    private LocalDateTime collectedAt;

    @Column(nullable = false, length = 64)
    private String source;

    public Long getId() {
        return id;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getRateToRub() {
        return rateToRub;
    }

    public void setRateToRub(BigDecimal rateToRub) {
        this.rateToRub = rateToRub;
    }

    public BigDecimal getRateToUsd() {
        return rateToUsd;
    }

    public void setRateToUsd(BigDecimal rateToUsd) {
        this.rateToUsd = rateToUsd;
    }

    public BigDecimal getDayChangePercent() {
        return dayChangePercent;
    }

    public void setDayChangePercent(BigDecimal dayChangePercent) {
        this.dayChangePercent = dayChangePercent;
    }

    public LocalDate getRateDate() {
        return rateDate;
    }

    public void setRateDate(LocalDate rateDate) {
        this.rateDate = rateDate;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
