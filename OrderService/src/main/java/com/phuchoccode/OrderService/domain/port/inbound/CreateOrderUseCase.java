package com.phuchoccode.OrderService.domain.port.inbound;

import com.phuchoccode.OrderService.domain.aggregate.OrderAggregate;
import com.phuchoccode.OrderService.request.CreateOrderRequest;

import java.util.Optional;

// domain/port/inbound/CreateOrderUseCase.java
public interface CreateOrderUseCase {
    OrderAggregate createOrder(CreateOrderRequest request);
    Optional<OrderAggregate> findById(String orderId);  // ← thêm
}
