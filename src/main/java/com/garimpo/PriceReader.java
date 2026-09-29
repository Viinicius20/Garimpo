package com.garimpo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class PriceReader {
    private final ObjectMapper mapper = new ObjectMapper();

    public BigDecimal read(String url) throws IOException {
        Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(15_000).maxBodySize(0).get();
        BigDecimal price = fromNextData(doc);
        if (price == null) price = fromLdJson(doc);
        if (price == null) price = fromMeta(doc);
        if (price == null) throw new IOException("Preço não encontrado (formato da loja não suportado)");
        return price;
    }

    private BigDecimal fromNextData(Document doc) {
        try {
            Element script = doc.getElementById("__NEXT_DATA__");
            if (script == null) return null;
            JsonNode product = mapper.readTree(script.data()).path("props").path("pageProps").path("product");
            JsonNode n = product.findValue("priceWithDiscount");
            BigDecimal p = n == null ? null : parse(n.asText());
            if (p == null || p.signum() <= 0) {
                n = product.findValue("price");
                p = n == null ? null : parse(n.asText());
            }
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal fromLdJson(Document doc) {
        for (Element s : doc.select("script[type=application/ld+json]")) {
            try {
                for (JsonNode parent : mapper.readTree(s.data()).findParents("offers")) {
                    JsonNode offers = parent.get("offers");
                    JsonNode n = offers.findValue("price");
                    if (n == null) n = offers.findValue("lowPrice");
                    BigDecimal p = n == null ? null : parse(n.asText());
                    if (p != null && p.signum() > 0) return p;
                }
            } catch (Exception ignored) {
                // tenta o próximo bloco
            }
        }
        return null;
    }

    private BigDecimal fromMeta(Document doc) {
        String sel = "meta[property=product:price:amount], meta[name=product:price:amount], "
                + "meta[property=og:price:amount], [itemprop=price]";
        for (Element e : doc.select(sel)) {
            BigDecimal p = parse(e.hasAttr("content") ? e.attr("content") : e.text());
            if (p != null && p.signum() > 0) return p;
        }
        return null;
    }

    static BigDecimal parse(String raw) {
        if (raw == null) return null;
        Matcher m = Pattern.compile("\\d[\\d.,]*").matcher(raw);
        if (!m.find()) return null;
        String s = m.group().replaceAll("[.,]+$", "");
        int comma = s.lastIndexOf(','), dot = s.lastIndexOf('.');
        if (comma >= 0 && dot >= 0) {                       // o último separador é o decimal
            s = comma > dot ? s.replace(".", "").replace(',', '.') : s.replace(",", "");
        } else if (comma >= 0) {
            s = s.replace(".", "").replace(',', '.');
        } else if (dot >= 0 && s.matches("\\d{1,3}(\\.\\d{3})+")) {   // "1.299" = milhar
            s = s.replace(".", "");
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}