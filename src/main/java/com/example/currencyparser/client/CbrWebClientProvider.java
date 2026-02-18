package com.example.currencyparser.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class CbrWebClientProvider implements ExternalRateProvider {

    private static final DateTimeFormatter CBR_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final WebClient webClient;

    public CbrWebClientProvider(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public String providerName() {
        return "CBR_WEBCLIENT";
    }

    @Override
    public Map<String, ParsedRate> loadRatesForDate(LocalDate date) {
        String url = "https://www.cbr.ru/scripts/XML_daily.asp?date_req=" + date.format(CBR_DATE_FORMAT);

        byte[] xmlBody = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();

        if (xmlBody == null || xmlBody.length == 0) {
            throw new IllegalStateException("Empty XML response from CBR via WebClient");
        }

        return CbrXmlParser.parse(xmlBody);
    }
}
