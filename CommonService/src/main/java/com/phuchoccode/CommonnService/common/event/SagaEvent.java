package com.phuchoccode.CommonnService.common.event;

public sealed interface SagaEvent permits InventoryReleasedEvent,
        InventoryReservedEvent,
        OrderCancelledEvent,
        OrderCreatedEvent,
        PaymentProcessedEvent,
        PaymentRefundedEvent,
        ShipmentStartedEvent {
}
