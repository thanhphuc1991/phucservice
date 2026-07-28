// domain/event/OrderPlacedEvent.java
package com.phuchoccode.OrderService.domain.event;

import java.time.Instant;

public record OrderPlacedEvent(
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
                orderId, customerId, productId,
                quantity, totalAmount, Instant.now()
        );
    }
}