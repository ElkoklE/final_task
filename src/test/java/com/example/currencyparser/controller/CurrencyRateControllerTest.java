package com.example.currencyparser.controller;

import com.example.currencyparser.dto.CurrencyRateResponse;
import com.example.currencyparser.service.CurrencyRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurrencyRateController.class)
class CurrencyRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyRateService service;

    @Test
    void shouldReturnRateByDateAndCurrency() throws Exception {
        CurrencyRateResponse response = new CurrencyRateResponse(
                "USD",
                new BigDecimal("100.123400"),
                new BigDecimal("1.000000"),
                new BigDecimal("0.1234"),
                LocalDate.of(2025, 11, 11),
                LocalDateTime.of(2025, 11, 11, 10, 0),
                "CBR_WEBCLIENT"
        );

        when(service.getByDateAndCurrency(eq(LocalDate.of(2025, 11, 11)), eq("USD")))
                .thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/v1/answer/by-date")
                        .param("date", "2025-11-11")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyCode").value("USD"));
    }

    @Test
    void shouldReturnSortedRates() throws Exception {
        when(service.getRatesForDateSorted(eq(LocalDate.of(2025, 11, 11)), eq("rateToRub"), eq("asc")))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/answer")
                        .param("date", "2025-11-11")
                        .param("sortBy", "rateToRub")
                        .param("direction", "asc"))
                .andExpect(status().isOk());
    }
}
