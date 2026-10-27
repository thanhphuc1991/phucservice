// domain/port/outbound/OrderRepositoryPort.java
package com.phuchoccode.OrderService.domain.port.outbound;

import com.phuchoccode.OrderService.domain.aggregate.OrderAggregate;
import com.phuchoccode.OrderService.domain.valueobject.OrderId;

import java.util.Optional;

public interface OrderRepositoryPort {
    OrderAggregate save(OrderAggregate aggregate);
    Optional<OrderAggregate> findById(OrderId orderId);
}