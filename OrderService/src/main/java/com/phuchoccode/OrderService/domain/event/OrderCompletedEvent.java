// domain/event/OrderCompletedEvent.java
package com.phuchoccode.OrderService.domain.event;

import java.time.Instant;

public record OrderCompletedEvent(
        String orderId,
        Instant occurredOn
) implements OrderDomainEvent {

    public static OrderCompletedEvent of(String orderId) {
        return new OrderCompletedEvent(orderId, Instant.now());
    }
}