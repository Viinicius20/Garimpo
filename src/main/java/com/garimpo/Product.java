package com.garimpo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Product {
    @Id @GeneratedValue public Long id;
    public String name;
    @Column(length = 2000) public String url;
    public BigDecimal targetPrice;    // avisa quando o preço chegar nisso ou menos
    public BigDecimal currentPrice;   // última leitura
    public LocalDateTime lastCheck;
    public boolean alerted;           // já avisou nesta queda? evita repetir o aviso
}
