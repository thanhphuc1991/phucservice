// infrastructure/persistence/entity/OutboxEvent.java
package com.phuchoccode.OrderService.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    private String id;

    private String aggregateId;    // orderId
    private String aggregateType;  // "Order"
    private String eventType;      // "OrderCreatedEvent"

    @Column(columnDefinition = "TEXT")
    private String payload;        // JSON của event

    private boolean sent;
    private int retryCount;
    private Instant createdAt;
    private Instant sentAt;

    public OutboxEvent(String id, String aggregateId, String aggregateType,
                       String eventType, String payload) {
        this.id            = id;
        this.aggregateId   = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType     = eventType;
        this.payload       = payload;
        this.sent          = false;
        this.retryCount    = 0;
        this.createdAt     = Instant.now();
    }
}