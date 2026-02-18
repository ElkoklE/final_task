package com.example.currencyparser.client;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class CbrXmlParser {

    private CbrXmlParser() {
    }

    public static Map<String, ParsedRate> parse(String xmlBody) {
        return parse(xmlBody.getBytes(StandardCharsets.UTF_8));
    }

    public static Map<String, ParsedRate> parse(byte[] xmlBytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlBytes));

            document.getDocumentElement().normalize();
            if (!"ValCurs".equals(document.getDocumentElement().getNodeName())) {
                String snippet = new String(xmlBytes, StandardCharsets.UTF_8);
                snippet = snippet.length() > 160 ? snippet.substring(0, 160) : snippet;
                throw new IllegalStateException("Unexpected root element: "
                        + document.getDocumentElement().getNodeName() + ", body starts with: " + snippet);
            }

            NodeList nodeList = document.getElementsByTagName("Valute");
            Map<String, ParsedRate> parsedRates = new HashMap<>();

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element element = (Element) node;
                String code = valueOf(element, "CharCode");
                int nominal = Integer.parseInt(valueOf(element, "Nominal"));

                BigDecimal currentValue = normalize(valueOf(element, "Value"));
                BigDecimal previousValue = normalize(valueOf(element, "Previous"));

                BigDecimal currentPerOne = currentValue.divide(BigDecimal.valueOf(nominal), 6, RoundingMode.HALF_UP);
                BigDecimal previousPerOne = previousValue.divide(BigDecimal.valueOf(nominal), 6, RoundingMode.HALF_UP);

                BigDecimal dayChange = currentPerOne
                        .subtract(previousPerOne)
                        .divide(previousPerOne, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                parsedRates.put(code, new ParsedRate(code, currentPerOne, dayChange));
            }

            return parsedRates;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse CBR XML response: " + e.getMessage(), e);
        }
    }

    private static String valueOf(Element element, String tagName) {
        return element.getElementsByTagName(tagName).item(0).getTextContent().trim();
    }

    private static BigDecimal normalize(String value) {
        return new BigDecimal(value.replace(',', '.'));
    }
}
