package com.garimpo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PriceReaderTest {

    // Se o seu PriceReader recebe algo no construtor (ex.: ObjectMapper), ajuste aqui.
    private final PriceReader reader = new PriceReader();

    private static void assertPreco(String esperado, BigDecimal real) {
        assertNotNull(real, "veio null, esperado " + esperado);
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " mas veio " + real);
    }

    private BigDecimal extrair(String html) throws IOException {
        return reader.extract(Jsoup.parse(html));
    }

    // ---------- parse: formatos de valor ----------

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "R$ 1.299,90|1299.90",
            "R$ 2.099,89|2099.89",
            "299,90|299.90",
            "1299.90|1299.90",
            "1,299.90|1299.90",
            "1.299|1299",
            "19.9|19.9",
            "Por R$ 799,99.|799.99"
    })
    void parseReconheceOsFormatosDeValor(String entrada, String esperado) {
        assertPreco(esperado, PriceReader.parse(entrada));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "grátis", "indisponível", "R$ --"})
    void parseSemNumeroDevolveNull(String entrada) {
        assertNull(PriceReader.parse(entrada));
    }

    @Test
    void parseDeNullDevolveNull() {
        assertNull(PriceReader.parse(null));
    }

    // ---------- JSON do Next.js ----------

    @Test
    void nextDataUsaOPrecoComDesconto() throws Exception {
        String html = """
                <html><head>
                <script id="__NEXT_DATA__" type="application/json">
                {"props":{"pageProps":{"product":{"price":"1.499,90","priceWithDiscount":"1.199,90"}}}}
                </script></head></html>
                """;
        assertPreco("1199.90", extrair(html));
    }

    @Test
    void nextDataCaiParaOPrecoCheioQuandoODescontoEZero() throws Exception {
        String html = """
                <html><head>
                <script id="__NEXT_DATA__" type="application/json">
                {"props":{"pageProps":{"product":{"price":1499.9,"priceWithDiscount":0}}}}
                </script></head></html>
                """;
        assertPreco("1499.9", extrair(html));
    }

    // ---------- JSON-LD ----------

    @Test
    void jsonLdLeOfertaSimples() throws Exception {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {"@context":"https://schema.org","@type":"Product","name":"Placa",
                 "offers":{"@type":"Offer","price":"799.99","priceCurrency":"BRL"}}
                </script></head></html>
                """;
        assertPreco("799.99", extrair(html));
    }

    @Test
    void jsonLdUsaLowPriceQuandoNaoHaPrice() throws Exception {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {"@type":"Product","offers":{"@type":"AggregateOffer","lowPrice":"699.00","highPrice":"899.00"}}
                </script></head></html>
                """;
        assertPreco("699.00", extrair(html));
    }

    @Test
    void jsonLdComOfertasEmLista() throws Exception {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {"@type":"Product","offers":[{"price":"10.50"},{"price":"12.00"}]}
                </script></head></html>
                """;
        assertPreco("10.50", extrair(html));
    }

    @Test
    void jsonLdIgnoraBlocoInvalidoETentaOProximo() throws Exception {
        String html = """
                <html><head>
                <script type="application/ld+json">{ isso nao e json</script>
                <script type="application/ld+json">{"@type":"Product","offers":{"price":"55.90"}}</script>
                </head></html>
                """;
        assertPreco("55.90", extrair(html));
    }

    // ---------- meta tags ----------

    @Test
    void metaProductPriceAmount() throws Exception {
        String html = """
                <html><head><meta property="product:price:amount" content="1.124,99"></head></html>
                """;
        assertPreco("1124.99", extrair(html));
    }

    @Test
    void itempropComTextoNoElemento() throws Exception {
        String html = """
                <html><body><span itemprop="price">R$ 311,34</span></body></html>
                """;
        assertPreco("311.34", extrair(html));
    }

    @Test
    void itempropComAtributoContent() throws Exception {
        String html = """
                <html><head><meta itemprop="price" content="300.00"></head></html>
                """;
        assertPreco("300.00", extrair(html));
    }

    // ---------- ordem das fontes e erros ----------

    @Test
    void nextDataTemPrioridadeSobreMeta() throws Exception {
        String html = """
                <html><head>
                <meta property="product:price:amount" content="999,00">
                <script id="__NEXT_DATA__" type="application/json">
                {"props":{"pageProps":{"product":{"price":"2.099,89"}}}}
                </script></head></html>
                """;
        assertPreco("2099.89", extrair(html));
    }

    @Test
    void ignoraPrecoZeroEUsaAProximaFonte() throws Exception {
        String html = """
                <html><head>
                <script type="application/ld+json">{"@type":"Product","offers":{"price":"0"}}</script>
                <meta property="product:price:amount" content="50,00">
                </head></html>
                """;
        assertPreco("50.00", extrair(html));
    }

    @Test
    void semPrecoLancaIOException() {
        IOException e = assertThrows(IOException.class,
                () -> extrair("<html><body>nada de preço aqui</body></html>"));
        assertTrue(e.getMessage().contains("Preço não encontrado"));
    }
}