package com.orderhub.api;

import com.orderhub.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import static com.orderhub.api.ApiModels.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }
    @PostMapping public ResponseEntity<OrderView> create(@Valid @RequestBody CreateOrder input, Authentication auth) {
        var result = service.create(input, auth);
        return ResponseEntity.created(URI.create("/api/orders/" + result.id())).body(result);
    }
    @GetMapping public PageView<OrderView> list(@RequestParam(required = false) OrderStatus status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size, Authentication auth) {
        return service.list(status, page, size, auth);
    }
    @GetMapping("/{id}") public OrderView get(@PathVariable long id, Authentication auth) { return service.get(id, auth); }
    @PatchMapping("/{id}/status") public OrderView change(@PathVariable long id, @Valid @RequestBody ChangeStatus input) {
        return service.change(id, input.status());
    }
}
