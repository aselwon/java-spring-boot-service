package com.orderhub.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "orders")
public class PurchaseOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version private long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.NEW;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderLine> lines = new ArrayList<>();
    protected PurchaseOrder() {}
    public PurchaseOrder(Customer customer) { this.customer = Objects.requireNonNull(customer); }
    public void addLine(Product product, int quantity) { lines.add(new OrderLine(this, product, quantity)); }
    public void transitionTo(OrderStatus next) { status.requireTransitionTo(next); status = next; }
    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderLine> getLines() { return Collections.unmodifiableList(lines); }
    public BigDecimal total() { return lines.stream().map(OrderLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add); }
}
