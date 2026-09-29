package com.garimpo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PriceJob {
    private static final Logger log = LoggerFactory.getLogger(PriceJob.class);
    private final ProductRepository products;
    private final PriceRecordRepository records;
    private final PriceReader reader;
    private final Notifier notifier;

    public PriceJob(ProductRepository products, PriceRecordRepository records, PriceReader reader, Notifier notifier) {
        this.products = products;
        this.records = records;
        this.reader = reader;
        this.notifier = notifier;
    }

    @Scheduled(initialDelay = 15_000, fixedRate = 6 * 60 * 60 * 1000L)   // a cada 6 horas
    public void runAll() {
        for (Product p : products.findAll()) {
            check(p);
            try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    }

    public void check(Product p) {
        try {
            BigDecimal price = reader.read(p.url);
            LocalDate today = LocalDate.now();

            PriceRecord r = records.findByProductIdAndDay(p.id, today).orElseGet(() -> {
                PriceRecord n = new PriceRecord();
                n.productId = p.id;
                n.day = today;
                n.price = price;
                return n;
            });
            if (price.compareTo(r.price) < 0) r.price = price;   // guarda o menor do dia
            records.save(r);

            p.currentPrice = price;
            p.lastCheck = LocalDateTime.now();
            boolean hit = p.targetPrice != null && price.compareTo(p.targetPrice) <= 0;
            if (hit && !p.alerted) {
                notifier.notify("Garimpo: chegou no preço", p.name + " está por R$ " + price);
            }
            p.alerted = hit;
            products.save(p);
        } catch (Exception e) {
            log.warn("Falha ao ler '{}': {}", p.name, e.getMessage());
        }
    }
}
