// infrastructure/messaging/KafkaEventPublisher.java
package com.phuchoccode.OrderService.infrastructure.messaging;

import com.phuchoccode.CommonnService.common.event.OrderCancelledEvent;
import com.phuchoccode.CommonnService.common.event.OrderCreatedEvent;
import com.phuchoccode.OrderService.domain.event.OrderCancelledDomainEvent;
import com.phuchoccode.OrderService.domain.event.OrderDomainEvent;
import com.phuchoccode.OrderService.domain.event.OrderPlacedEvent;
import com.phuchoccode.OrderService.domain.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(List<OrderDomainEvent> events) {
        events.forEach(this::publish);
    }

    public void publish(OrderDomainEvent event) {
        switch (event) {

            case OrderPlacedEvent e -> {
                kafkaTemplate.send("order.created", e.orderId(),
                        new OrderCreatedEvent(
                                e.orderId(),
                                e.customerId(),
                                e.productId(),
                                e.quantity(),
                                e.totalAmount()
                        )
                );
                log.info("Published OrderCreatedEvent — orderId={}", e.orderId());
            }

            case OrderCancelledDomainEvent e -> {
                kafkaTemplate.send("order.cancelled", e.orderId(),
                        new OrderCancelledEvent(
                                e.orderId(),
                                e.reason(),
                                e.failedStep()
                        )
                );
                log.warn("Published OrderCancelledEvent — orderId={}, step={}",
                        e.orderId(), e.failedStep());
            }

            case OrderCompletedEvent e ->
                    log.info("Order {} completed — no Kafka event needed", e.orderId());
        }
    }
}