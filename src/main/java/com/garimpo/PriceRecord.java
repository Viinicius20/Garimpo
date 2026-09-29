package com.garimpo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class PriceRecord {
    @Id @GeneratedValue public Long id;
    public Long productId;
    @Column(name = "record_day") public LocalDate day;
    public BigDecimal price;
}