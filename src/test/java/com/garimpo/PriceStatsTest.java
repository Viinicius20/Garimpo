package com.garimpo; // ajuste para o pacote do seu projeto

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PriceStatsTest {

    private static PriceRecord rec(String price) {
        PriceRecord r = new PriceRecord();
        r.price = new BigDecimal(price);
        r.day = LocalDate.now();
        return r;
    }

    private static void assertPreco(String esperado, BigDecimal real) {
        assertNotNull(real, "veio null, esperado " + esperado);
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " mas veio " + real);
    }

    @Test
    void semRegistrosNaoTemMediaNemMinimo() {
        PriceStats s = PriceStats.of(List.of());
        assertNull(s.avg());
        assertNull(s.min());
        assertEquals(0, s.days());
    }

    @Test
    void umRegistroTemMediaEMinimoIguaisAoPreco() {
        PriceStats s = PriceStats.of(List.of(rec("2099.89")));
        assertPreco("2099.89", s.avg());
        assertPreco("2099.89", s.min());
        assertEquals(1, s.days());
    }

    @Test
    void calculaMediaEMinimoDeVariosDias() {
        PriceStats s = PriceStats.of(List.of(rec("100.00"), rec("120.00"), rec("110.00")));
        assertPreco("110.00", s.avg());
        assertPreco("100.00", s.min());
        assertEquals(3, s.days());
    }

    @Test
    void mediaArredondaMeioParaCima() {
        // (10.00 + 10.01) / 2 = 10.005 -> 10.01
        PriceStats s = PriceStats.of(List.of(rec("10.00"), rec("10.01")));
        assertPreco("10.01", s.avg());
    }

    @Test
    void mediaComDizimaPeriodicaFicaComDuasCasas() {
        // 301 / 3 = 100.333... -> 100.33
        PriceStats s = PriceStats.of(List.of(rec("100.00"), rec("100.00"), rec("101.00")));
        assertPreco("100.33", s.avg());
    }
}