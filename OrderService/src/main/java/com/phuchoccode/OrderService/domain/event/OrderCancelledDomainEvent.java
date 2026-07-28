// domain/event/OrderCancelledDomainEvent.java
package com.phuchoccode.OrderService.domain.event;

import java.time.Instant;

public record OrderCancelledDomainEvent(
        String orderId,
        String reason,
        String failedStep,
        Instant occurredOn
) implements OrderDomainEvent {

    public static OrderCancelledDomainEvent of(String orderId,
                                               String reason,
                                               String failedStep) {
        return new OrderCancelledDomainEvent(orderId, reason, failedStep, Instant.now());
    }
}