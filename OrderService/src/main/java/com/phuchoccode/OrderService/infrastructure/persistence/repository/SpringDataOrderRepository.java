package com.phuchoccode.OrderService.infrastructure.persistence.repository;

import com.phuchoccode.OrderService.infrastructure.persistence.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataOrderRepository
        extends JpaRepository<Order, String> {
}
