package com.orderhub.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String sku;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    protected Product() {}
    public Product(String sku, String name, BigDecimal price) {
        this.sku = sku; this.name = name; this.price = price;
    }
    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
}
