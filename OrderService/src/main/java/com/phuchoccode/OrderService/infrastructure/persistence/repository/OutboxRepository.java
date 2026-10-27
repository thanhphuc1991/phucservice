// infrastructure/persistence/repository/OutboxRepository.java
package com.phuchoccode.OrderService.infrastructure.persistence.repository;

import com.phuchoccode.OrderService.infrastructure.persistence.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {

    // Lấy tất cả event chưa gửi, retry < 3
    List<OutboxEvent> findBySentFalseAndRetryCountLessThan(int maxRetry);
}