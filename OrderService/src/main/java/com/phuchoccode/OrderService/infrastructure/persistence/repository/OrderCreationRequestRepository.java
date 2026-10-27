package com.phuchoccode.OrderService.infrastructure.persistence.repository;

import com.phuchoccode.OrderService.infrastructure.persistence.entity.OrderCreationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderCreationRequestRepository
        extends JpaRepository<OrderCreationRequest, String> {
}