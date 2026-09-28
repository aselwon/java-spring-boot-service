package com.orderhub.api;

import com.orderhub.domain.*;
import com.orderhub.infrastructure.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashSet;
import static com.orderhub.api.ApiModels.*;

@Service
@Transactional
public class OrderService {
    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final ProductRepository products;
    public OrderService(OrderRepository orders, CustomerRepository customers, ProductRepository products) {
        this.orders = orders; this.customers = customers; this.products = products;
    }
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public OrderView create(CreateOrder input, Authentication auth) {
        Customer customer = customers.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer profile required"));
        var order = new PurchaseOrder(customer);
        var seen = new HashSet<Long>();
        for (var line : input.lines()) {
            if (!seen.add(line.productId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate product in order");
            order.addLine(products.findById(line.productId()).orElseThrow(() -> missing("Product")), line.quantity());
        }
        return OrderView.from(orders.saveAndFlush(order));
    }
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public OrderView get(long id, Authentication auth) { return OrderView.from(ownedOrder(id, auth)); }
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public PageView<OrderView> list(OrderStatus status, int page, int size, Authentication auth) {
        Specification<PurchaseOrder> spec = (root, query, cb) -> cb.conjunction();
        if (!isAdmin(auth)) spec = spec.and((root, query, cb) -> cb.equal(root.get("customer").get("username"), auth.getName()));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        var result = orders.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return new PageView<>(result.getContent().stream().map(OrderView::from).toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }
    @PreAuthorize("hasRole('ADMIN')")
    public OrderView change(long id, OrderStatus next) {
        var order = orders.findById(id).orElseThrow(() -> missing("Order"));
        order.transitionTo(next);
        orders.flush();
        return OrderView.from(order);
    }
    private PurchaseOrder ownedOrder(long id, Authentication auth) {
        var order = orders.findById(id).orElseThrow(() -> missing("Order"));
        if (!isAdmin(auth) && !order.getCustomer().getUsername().equals(auth.getName())) throw missing("Order");
        return order;
    }
    private boolean isAdmin(Authentication auth) { return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")); }
    private static ResponseStatusException missing(String resource) { return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found"); }
}
