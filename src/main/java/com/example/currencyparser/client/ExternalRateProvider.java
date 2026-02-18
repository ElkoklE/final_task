package com.example.currencyparser.client;

import java.time.LocalDate;
import java.util.Map;

public interface ExternalRateProvider {

    String providerName();

    Map<String, ParsedRate> loadRatesForDate(LocalDate date);
}
