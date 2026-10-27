package com.phuchoccode.OrderService.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;    // ← fix import
import com.phuchoccode.CommonService.common.event.OrderCancelledEvent;
import com.phuchoccode.CommonService.common.event.OrderCreatedEvent;
import com.phuchoccode.OrderService.domain.event.*;
import com.phuchoccode.OrderService.infrastructure.persistence.entity.OutboxEvent;
import com.phuchoccode.OrderService.infrastructure.persistence.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRY = 3;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void relay() {
        List<OutboxEvent> pendingEvents = outboxRepository
                .findBySentFalseAndRetryCountLessThan(MAX_RETRY);

        if (pendingEvents.isEmpty()) return;

        log.info("OutboxRelay — processing {} pending events", pendingEvents.size());
        pendingEvents.forEach(this::sendToKafka);
    }

    private void sendToKafka(OutboxEvent outboxEvent) {
        try {
            Object kafkaEvent = deserializeEvent(outboxEvent);
            String topic      = resolveTopic(outboxEvent.getEventType());

            kafkaTemplate.send(topic, outboxEvent.getAggregateId(), kafkaEvent);

            outboxEvent.setSent(true);
            outboxEvent.setSentAt(Instant.now());
            outboxRepository.save(outboxEvent);

            log.info("OutboxRelay sent — eventType={}, orderId={}",
                    outboxEvent.getEventType(), outboxEvent.getAggregateId());

        } catch (Exception e) {
            outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
            outboxRepository.save(outboxEvent);
            log.error("OutboxRelay failed — eventType={}, retry={}, error={}",
                    outboxEvent.getEventType(), outboxEvent.getRetryCount(), e.getMessage());
        }
    }

    private Object deserializeEvent(OutboxEvent outboxEvent) throws Exception {
        return switch (outboxEvent.getEventType()) {

            case "OrderPlacedEvent" -> {
                var e = objectMapper.readValue(outboxEvent.getPayload(), OrderPlacedEvent.class);
                yield new OrderCreatedEvent(
                        e.eventId(),       // ← dùng eventId gốc, không tạo mới
                        e.orderId(),
                        e.customerId(),
                        e.productId(),
                        e.quantity(),
                        e.totalAmount()
                );
            }

            case "OrderCancelledDomainEvent" -> {
                var e = objectMapper.readValue(outboxEvent.getPayload(), OrderCancelledDomainEvent.class);
                yield new OrderCancelledEvent(
                        e.eventId(),       // ← dùng eventId gốc
                        e.orderId(),
                        e.reason(),
                        e.failedStep()
                );
            }

            case "OrderCompletedEvent" -> {
                // OrderCompletedEvent không publish lên Kafka
                // chỉ log — không cần deserialize
                throw new IllegalArgumentException("OrderCompletedEvent is not published to Kafka");
            }

            default -> throw new IllegalArgumentException(
                    "Unknown event type: " + outboxEvent.getEventType()
            );
        };
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "OrderPlacedEvent"          -> "order.created";
            case "OrderCancelledDomainEvent" -> "order.cancelled";
            default -> throw new IllegalArgumentException("Unknown topic for: " + eventType);
        };
    }
}