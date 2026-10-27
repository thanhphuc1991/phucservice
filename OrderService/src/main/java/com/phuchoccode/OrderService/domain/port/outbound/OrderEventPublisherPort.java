// domain/port/outbound/OrderEventPublisherPort.java
package com.phuchoccode.OrderService.domain.port.outbound;

import com.phuchoccode.OrderService.domain.event.OrderDomainEvent;

import java.util.List;

public interface OrderEventPublisherPort {
    void publish(List<OrderDomainEvent> events);
    void publish(OrderDomainEvent event);
}