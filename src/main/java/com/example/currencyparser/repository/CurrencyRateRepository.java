package com.example.currencyparser.repository;

import com.example.currencyparser.model.CurrencyRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long> {

    List<CurrencyRate> findByRateDate(LocalDate rateDate);

    Optional<CurrencyRate> findTopByRateDateAndCurrencyCodeIgnoreCaseOrderByCollectedAtDesc(LocalDate rateDate, String currencyCode);
}
