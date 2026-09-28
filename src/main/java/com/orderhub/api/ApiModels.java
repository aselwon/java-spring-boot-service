package com.orderhub.api;

import com.orderhub.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiModels {
    private ApiModels() {}
    public record LineRequest(@NotNull @Positive Long productId, @Min(1) @Max(1000) int quantity) {}
    public record CreateOrder(@NotEmpty @Size(max = 100) List<@NotNull @Valid LineRequest> lines) {}
    public record ChangeStatus(@NotNull OrderStatus status) {}
    public record CreateProduct(@NotBlank @Size(max = 64) String sku,
            @NotBlank @Size(max = 200) String name,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price) {}
    public record CustomerView(Long id, String username, String name, String email) {
        public static CustomerView from(Customer c) { return new CustomerView(c.getId(), c.getUsername(), c.getName(), c.getEmail()); }
    }
    public record ProductView(Long id, String sku, String name, BigDecimal price) {
        public static ProductView from(Product p) { return new ProductView(p.getId(), p.getSku(), p.getName(), p.getPrice()); }
    }
    public record LineView(Long productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
    public record OrderView(Long id, Long customerId, OrderStatus status, Instant createdAt,
                            String currency, BigDecimal total, List<LineView> lines) {
        public static OrderView from(PurchaseOrder o) {
            return new OrderView(o.getId(), o.getCustomer().getId(), o.getStatus(), o.getCreatedAt(), "PLN", o.total(),
                o.getLines().stream().map(l -> new LineView(l.getProduct().getId(), l.getProduct().getName(),
                    l.getQuantity(), l.getUnitPrice(), l.subtotal())).toList());
        }
    }
    public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
