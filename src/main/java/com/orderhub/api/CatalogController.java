package com.orderhub.api;

import com.orderhub.domain.Product;
import com.orderhub.infrastructure.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.List;
import static com.orderhub.api.ApiModels.*;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final ProductRepository products;
    private final CustomerRepository customers;
    public CatalogController(ProductRepository products, CustomerRepository customers) { this.products = products; this.customers = customers; }
    @GetMapping("/products") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<ProductView> products() { return products.findAll().stream().map(ProductView::from).toList(); }
    @PostMapping("/products") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductView> create(@Valid @RequestBody CreateProduct input) {
        var product = products.saveAndFlush(new Product(input.sku(), input.name(), input.price()));
        return ResponseEntity.created(URI.create("/api/products/" + product.getId())).body(ProductView.from(product));
    }
    @GetMapping("/products/{id}") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ProductView product(@PathVariable long id) { return ProductView.from(products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"))); }
    @GetMapping("/customers/me") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public CustomerView me(Authentication auth) { return CustomerView.from(customers.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"))); }
    @GetMapping("/customers") @PreAuthorize("hasRole('ADMIN')")
    public List<CustomerView> customers() { return customers.findAll().stream().map(CustomerView::from).toList(); }
}
