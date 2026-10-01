package com.garimpo; // ajuste para o pacote do seu projeto

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PriceJobTest {

    private ProductRepository products;
    private PriceRecordRepository records;
    private PriceReader reader;
    private Notifier notifier;
    private PriceJob job;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        records = mock(PriceRecordRepository.class);
        reader = mock(PriceReader.class);
        notifier = mock(Notifier.class);
        job = new PriceJob(products, records, reader, notifier);
        when(records.findByProductIdAndDay(any(), any())).thenReturn(Optional.empty());
    }

    private Product produto(String meta, boolean jaAvisou) {
        Product p = new Product();
        p.id = 1L;
        p.name = "Cooler";
        p.url = "https://loja.exemplo/cooler";
        p.targetPrice = meta == null ? null : new BigDecimal(meta);
        p.alerted = jaAvisou;
        return p;
    }

    // ---------- regra de aviso ----------

    @Test
    void avisaQuandoPrecoChegaNaMeta() throws Exception {
        Product p = produto("300.00", false);
        when(reader.read(p.url)).thenReturn(new BigDecimal("299.90"));

        job.check(p);

        verify(notifier).notify(eq("Garimpo: chegou no preço"), contains("Cooler"));
        assertTrue(p.alerted);
        verify(products).save(p);
    }

    @Test
    void avisaQuandoPrecoEIgualAMeta() throws Exception {
        Product p = produto("300.00", false);
        when(reader.read(p.url)).thenReturn(new BigDecimal("300.00"));

        job.check(p);

        verify(notifier).notify(anyString(), anyString());
        assertTrue(p.alerted);
    }

    @Test
    void naoAvisaQuandoPrecoEstaAcimaDaMeta() throws Exception {
        Product p = produto("300.00", false);
        when(reader.read(p.url)).thenReturn(new BigDecimal("311.34"));

        job.check(p);

        verify(notifier, never()).notify(anyString(), anyString());
        assertFalse(p.alerted);
    }

    @Test
    void naoRepeteOAvisoEnquantoPermaneceNaMeta() throws Exception {
        Product p = produto("300.00", true);
        when(reader.read(p.url)).thenReturn(new BigDecimal("290.00"));

        job.check(p);

        verify(notifier, never()).notify(anyString(), anyString());
        assertTrue(p.alerted);
    }

    @Test
    void rearmaQuandoPrecoVoltaAcimaDaMeta() throws Exception {
        Product p = produto("300.00", true);
        when(reader.read(p.url)).thenReturn(new BigDecimal("350.00"));

        job.check(p);

        verify(notifier, never()).notify(anyString(), anyString());
        assertFalse(p.alerted);
    }

    @Test
    void avisaDeNovoDepoisDeRearmar() throws Exception {
        Product p = produto("300.00", false);
        when(reader.read(p.url)).thenReturn(
                new BigDecimal("290.00"),   // bateu a meta -> avisa
                new BigDecimal("350.00"),   // subiu -> rearma
                new BigDecimal("280.00"));  // bateu de novo -> avisa outra vez

        job.check(p);
        job.check(p);
        job.check(p);

        verify(notifier, times(2)).notify(anyString(), anyString());
        assertTrue(p.alerted);
    }

    @Test
    void semMetaNuncaAvisa() throws Exception {
        Product p = produto(null, false);
        when(reader.read(p.url)).thenReturn(new BigDecimal("1.00"));

        job.check(p);

        verify(notifier, never()).notify(anyString(), anyString());
        assertFalse(p.alerted);
    }

    // ---------- atualização do produto ----------

    @Test
    void atualizaPrecoAtualEUltimaLeitura() throws Exception {
        Product p = produto("300.00", false);
        when(reader.read(p.url)).thenReturn(new BigDecimal("311.34"));

        job.check(p);

        assertEquals(new BigDecimal("311.34"), p.currentPrice);
        assertNotNull(p.lastCheck);
    }

    // ---------- registro diário ----------

    @Test
    void criaORegistroDoDiaQuandoNaoExiste() throws Exception {
        Product p = produto("300.00", false);
        when(reader.read(p.url)).thenReturn(new BigDecimal("311.34"));

        job.check(p);

        ArgumentCaptor<PriceRecord> captor = ArgumentCaptor.forClass(PriceRecord.class);
        verify(records).save(captor.capture());
        PriceRecord salvo = captor.getValue();
        assertEquals(1L, salvo.productId);
        assertEquals(LocalDate.now(), salvo.day);
        assertEquals(0, new BigDecimal("311.34").compareTo(salvo.price));
    }

    @Test
    void mantemOMenorPrecoDoDiaQuandoOPrecoSobe() throws Exception {
        Product p = produto("50.00", false);
        PriceRecord existente = new PriceRecord();
        existente.productId = 1L;
        existente.day = LocalDate.now();
        existente.price = new BigDecimal("100.00");
        when(records.findByProductIdAndDay(any(), any())).thenReturn(Optional.of(existente));
        when(reader.read(p.url)).thenReturn(new BigDecimal("120.00"));

        job.check(p);

        assertEquals(0, new BigDecimal("100.00").compareTo(existente.price)); // registro do dia continua no menor
        assertEquals(new BigDecimal("120.00"), p.currentPrice);               // mas o preço atual é o da última leitura
    }

    @Test
    void baixaORegistroDoDiaQuandoOPrecoCai() throws Exception {
        Product p = produto("50.00", false);
        PriceRecord existente = new PriceRecord();
        existente.productId = 1L;
        existente.day = LocalDate.now();
        existente.price = new BigDecimal("100.00");
        when(records.findByProductIdAndDay(any(), any())).thenReturn(Optional.of(existente));
        when(reader.read(p.url)).thenReturn(new BigDecimal("90.00"));

        job.check(p);

        assertEquals(0, new BigDecimal("90.00").compareTo(existente.price));
        verify(records).save(existente);
    }

    // ---------- falhas ----------

    @Test
    void falhaNaLeituraNaoQuebraENaoAlteraNada() throws Exception {
        Product p = produto("300.00", false);
        when(reader.read(p.url)).thenThrow(new IOException("loja fora do ar"));

        assertDoesNotThrow(() -> job.check(p));

        verify(notifier, never()).notify(anyString(), anyString());
        verify(products, never()).save(any());
        verify(records, never()).save(any());
    }
}