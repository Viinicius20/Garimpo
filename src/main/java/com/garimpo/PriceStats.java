package com.garimpo; // ajuste para o pacote do seu projeto

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/** Média, menor preço e quantidade de dias de uma lista de registros de preço. */
public record PriceStats(BigDecimal avg, BigDecimal min, int days) {

    public static PriceStats of(List<PriceRecord> records) {
        if (records.isEmpty()) return new PriceStats(null, null, 0);
        BigDecimal avg = records.stream().map(r -> r.price).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);
        BigDecimal min = records.stream().map(r -> r.price).min(Comparator.naturalOrder()).get();
        return new PriceStats(avg, min, records.size());
    }
}