package com.garimpo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    record NewProduct(String name, String url, BigDecimal targetPrice) {}
    record View(Long id, String name, String url, BigDecimal target, BigDecimal current,
                BigDecimal avg90, BigDecimal min90, int days, LocalDateTime lastCheck) {}

    private final ProductRepository products;
    private final PriceRecordRepository records;
    private final PriceJob job;

    public ProductController(ProductRepository products, PriceRecordRepository records, PriceJob job) {
        this.products = products;
        this.records = records;
        this.job = job;
    }

    private View view(Product p) {
        List<PriceRecord> rs = records.findByProductIdAndDayGreaterThanEqual(p.id, LocalDate.now().minusDays(90));
        PriceStats s = PriceStats.of(rs);
        return new View(p.id, p.name, p.url, p.targetPrice, p.currentPrice, s.avg(), s.min(), s.days(), p.lastCheck);
    }

    @GetMapping
    public List<View> list() {
        return products.findAll().stream().map(this::view).toList();
    }

    @PostMapping
    public View add(@RequestBody NewProduct body) {
        if (body.name() == null || body.name().isBlank() || body.url() == null
                || !body.url().startsWith("https://") || body.targetPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name, url (https) e targetPrice são obrigatórios");
        }
        Product p = new Product();
        p.name = body.name().trim();
        p.url = body.url().trim();
        p.targetPrice = body.targetPrice();
        p = products.save(p);
        job.check(p);   // já lê o preço na hora, para a tela não ficar vazia
        return view(p);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void remove(@PathVariable Long id) {
        records.deleteByProductId(id);
        products.deleteById(id);
    }
}
