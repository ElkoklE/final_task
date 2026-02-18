package com.example.currencyparser.service;

import com.example.currencyparser.client.CbrXmlParser;
import com.example.currencyparser.client.ParsedRate;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CbrXmlParserTest {

    @Test
    void shouldParseRatesAndDailyChanges() {
        String xml = """
                <?xml version=\"1.0\" encoding=\"windows-1251\"?>
                <ValCurs Date=\"11.11.2025\" name=\"Foreign Currency Market\">
                    <Valute ID=\"R01235\">
                        <NumCode>840</NumCode>
                        <CharCode>USD</CharCode>
                        <Nominal>1</Nominal>
                        <Name>US Dollar</Name>
                        <Value>100,0000</Value>
                        <Previous>99,0000</Previous>
                    </Valute>
                    <Valute ID=\"R01239\">
                        <NumCode>978</NumCode>
                        <CharCode>EUR</CharCode>
                        <Nominal>1</Nominal>
                        <Name>Euro</Name>
                        <Value>108,0000</Value>
                        <Previous>107,0000</Previous>
                    </Valute>
                </ValCurs>
                """;

        Map<String, ParsedRate> parsed = CbrXmlParser.parse(xml);

        assertEquals("100.000000", parsed.get("USD").rateToRub().toPlainString());
        assertEquals("108.000000", parsed.get("EUR").rateToRub().toPlainString());
    }
}
