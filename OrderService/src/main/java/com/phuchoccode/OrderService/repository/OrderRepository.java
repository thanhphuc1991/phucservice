package com.phuchoccode.OrderService.repository;

import com.phuchoccode.OrderService.entity.Order;
import com.phuchoccode.OrderService.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByCustomerId(String customerId);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByIdAndStatus(String id, OrderStatus status);
}
