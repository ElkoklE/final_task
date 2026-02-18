package com.example.currencyparser.client;

import org.springframework.stereotype.Component;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class CbrRestTemplateProvider implements ExternalRateProvider {

    private static final DateTimeFormatter CBR_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final RestTemplate restTemplate;

    public CbrRestTemplateProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String providerName() {
        return "CBR_RESTTEMPLATE";
    }

    @Override
    public Map<String, ParsedRate> loadRatesForDate(LocalDate date) {
        String url = "https://www.cbr.ru/scripts/XML_daily.asp?date_req=" + date.format(CBR_DATE_FORMAT);
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        byte[] xmlBody = response.getBody();

        if (xmlBody == null || xmlBody.length == 0) {
            throw new IllegalStateException("Empty XML response from CBR via RestTemplate");
        }

        return CbrXmlParser.parse(xmlBody);
    }
}
