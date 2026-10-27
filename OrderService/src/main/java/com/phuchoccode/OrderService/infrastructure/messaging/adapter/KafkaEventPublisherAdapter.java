package com.phuchoccode.OrderService.infrastructure.messaging.adapter;

import com.phuchoccode.CommonService.common.event.OrderCancelledEvent;
import com.phuchoccode.CommonService.common.event.OrderCreatedEvent;
import com.phuchoccode.OrderService.domain.event.*;
import com.phuchoccode.OrderService.domain.port.outbound.OrderEventPublisherPort;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisherAdapter implements OrderEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(List<OrderDomainEvent> events) {
        events.forEach(this::publish);
    }

    @Override
    @Retry(name = "kafkaPublisher", fallbackMethod = "publishFallback")
    public void publish(OrderDomainEvent event) {
        switch (event) {
            case OrderPlacedEvent e -> {
                kafkaTemplate.send("order.created", e.orderId(),
                        new OrderCreatedEvent(
                                e.eventId(),     // ← dùng eventId gốc
                                e.orderId(),
                                e.customerId(),
                                e.productId(),
                                e.quantity(),
                                e.totalAmount()
                        )
                );
                log.info("Published OrderCreatedEvent — orderId={}, eventId={}",
                        e.orderId(), e.eventId());
            }

            case OrderCancelledDomainEvent e -> {
                kafkaTemplate.send("order.cancelled", e.orderId(),
                        new OrderCancelledEvent(
                                e.eventId(),     // ← dùng eventId gốc
                                e.orderId(),
                                e.reason(),
                                e.failedStep()
                        )
                );
                log.warn("Published OrderCancelledEvent — orderId={}, eventId={}",
                        e.orderId(), e.eventId());
            }

            case OrderCompletedEvent e ->
                    log.info("Order {} completed — eventId={}", e.orderId(), e.eventId());
        }
    }

    public void publishFallback(OrderDomainEvent event, Exception ex) {
        log.error("Failed to publish event after retries — eventId={}, error={}",
                event.eventId(), ex.getMessage());
    }
}