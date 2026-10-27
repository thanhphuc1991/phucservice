package com.phuchoccode.OrderService.domain.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledDomainEvent(
        String eventId,        // ← thêm
        String orderId,
        String reason,
        String failedStep,
        Instant occurredOn
) implements OrderDomainEvent {

    public static OrderCancelledDomainEvent of(String orderId,
                                               String reason,
                                               String failedStep) {
        return new OrderCancelledDomainEvent(
                UUID.randomUUID().toString(),  // ← tạo 1 lần duy nhất
                orderId, reason, failedStep, Instant.now()
        );
    }
}