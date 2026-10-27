package com.phuchoccode.OrderService.domain.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCompletedEvent(
        String eventId,        // ← thêm
        String orderId,
        Instant occurredOn
) implements OrderDomainEvent {

    public static OrderCompletedEvent of(String orderId) {
        return new OrderCompletedEvent(
                UUID.randomUUID().toString(),  // ← tạo 1 lần duy nhất
                orderId, Instant.now()
        );
    }
}