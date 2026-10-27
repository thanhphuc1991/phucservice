package com.phuchoccode.OrderService.domain.event;

import java.time.Instant;
import java.util.UUID;

public record OrderPlacedEvent(
        String eventId,        // ← thêm
        String orderId,
        String customerId,
        String productId,
        int quantity,
        double totalAmount,
        Instant occurredOn
) implements OrderDomainEvent {

    public static OrderPlacedEvent of(String orderId, String customerId,
                                      String productId, int quantity,
                                      double totalAmount) {
        return new OrderPlacedEvent(
                UUID.randomUUID().toString(),  // ← tạo 1 lần duy nhất
                orderId, customerId, productId,
                quantity, totalAmount, Instant.now()
        );
    }
}