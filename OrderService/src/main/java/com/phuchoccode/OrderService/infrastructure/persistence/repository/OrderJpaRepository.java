// infrastructure/persistence/repository/OrderJpaRepository.java
package com.phuchoccode.OrderService.infrastructure.persistence.repository;

import com.phuchoccode.OrderService.infrastructure.persistence.entity.Order;
import com.phuchoccode.OrderService.domain.valueobject.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, String> {

    List<Order> findByCustomerId(String customerId);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByIdAndStatus(String id, OrderStatus status);
}