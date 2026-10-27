package com.phuchoccode.OrderService.infrastructure.web;

import com.phuchoccode.OrderService.application.service.IdempotentOrderCreationService;
import com.phuchoccode.OrderService.domain.port.inbound.CreateOrderUseCase;
import com.phuchoccode.OrderService.request.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final IdempotentOrderCreationService idempotentOrderCreationService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestHeader("Idempotency-Key") String key, @RequestBody CreateOrderRequest request) {
        var response = idempotentOrderCreationService.create(key, request);
        var order = createOrderUseCase.createOrder(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        return createOrderUseCase.findById(orderId)
                .map(order -> ResponseEntity.ok(new OrderResponse(
                        order.orderId().value(),
                        order.customerId().value(),
                        order.status().name(),
                        order.totalAmount().amount().doubleValue()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}