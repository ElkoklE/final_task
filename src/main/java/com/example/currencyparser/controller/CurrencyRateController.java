package com.example.currencyparser.controller;

import com.example.currencyparser.dto.CurrencyRateResponse;
import com.example.currencyparser.service.CurrencyRateService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CurrencyRateController {

    private final CurrencyRateService currencyRateService;

    public CurrencyRateController(CurrencyRateService currencyRateService) {
        this.currencyRateService = currencyRateService;
    }

    @PostMapping("/collect")
    public Map<String, String> collect(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        currencyRateService.triggerCollection(targetDate);
        return Map.of("message", "Collection started", "date", targetDate.toString());
    }

    @GetMapping("/answer")
    public List<CurrencyRateResponse> answer(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "currencyCode") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        return currencyRateService.getRatesForDateSorted(targetDate, sortBy, direction);
    }

    @GetMapping("/answer/by-date")
    public CurrencyRateResponse byDateAndCurrency(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String currency
    ) {
        return currencyRateService.getByDateAndCurrency(date, currency)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No rate for date=" + date + ", currency=" + currency
                ));
    }

    @GetMapping("/status")
    public Map<String, String> status() {
        return currencyRateService.getCollectionStatus();
    }
}
