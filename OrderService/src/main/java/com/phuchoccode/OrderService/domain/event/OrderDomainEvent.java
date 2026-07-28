// domain/event/OrderDomainEvent.java
package com.phuchoccode.OrderService.domain.event;

import java.time.Instant;

public sealed interface OrderDomainEvent
        permits OrderPlacedEvent, OrderCancelledDomainEvent, OrderCompletedEvent {
    String orderId();
    Instant occurredOn();
}