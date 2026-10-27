package com.phuchoccode.CommonService.common.event;

public record OrderCreatedEvent(
        String eventId,
        String orderId,
        String customerId,
        String productId,
        int quantity,
        double totalAmount
) implements SagaEvent {
}
