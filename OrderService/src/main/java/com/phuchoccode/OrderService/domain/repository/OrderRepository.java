package com.phuchoccode.OrderService.domain.repository;

import com.phuchoccode.OrderService.domain.aggregate.OrderAggregate;
import com.phuchoccode.OrderService.domain.valueobject.OrderId;

import java.util.Optional;

public interface OrderRepository {

    OrderAggregate save(OrderAggregate aggregate);

    Optional<OrderAggregate> findById(OrderId orderId);

    boolean exists(OrderId orderId);

    void delete(OrderId orderId);
}