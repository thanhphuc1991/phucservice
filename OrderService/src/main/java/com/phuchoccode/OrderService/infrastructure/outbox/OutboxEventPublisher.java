package com.phuchoccode.OrderService.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;    // ← fix import
import com.phuchoccode.OrderService.domain.event.OrderDomainEvent;
import com.phuchoccode.OrderService.domain.port.outbound.OrderEventPublisherPort;
import com.phuchoccode.OrderService.infrastructure.persistence.entity.OutboxEvent;
import com.phuchoccode.OrderService.infrastructure.persistence.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher implements OrderEventPublisherPort {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(List<OrderDomainEvent> events) {
        events.forEach(this::publish);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(OrderDomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            var outboxEvent = new OutboxEvent(
                    UUID.randomUUID().toString(),  // outboxEvent ID — khác với eventId
                    event.orderId(),
                    "Order",
                    event.getClass().getSimpleName(),
                    payload
            );

            outboxRepository.save(outboxEvent);
            log.info("Saved to outbox — eventType={}, orderId={}, eventId={}",
                    event.getClass().getSimpleName(), event.orderId(), event.eventId());

        } catch (Exception e) {
            log.error("Failed to save outbox event", e);
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }
}