package com.example.currencyparser.service;

import com.example.currencyparser.model.CurrencyRate;
import com.example.currencyparser.repository.CurrencyRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CurrencyRatePersistenceService {

    private final CurrencyRateRepository repository;

    public CurrencyRatePersistenceService(CurrencyRateRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int saveAllInTransaction(List<CurrencyRate> entities) {
        repository.saveAll(entities);
        return entities.size();
    }
}
