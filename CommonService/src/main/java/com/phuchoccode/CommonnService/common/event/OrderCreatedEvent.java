package com.phuchoccode.CommonnService.common.event;

public record OrderCreatedEvent(
        String orderId,
        String customerId,
        String productId,
        int quantity,
        double totalAmount
) implements SagaEvent {
}
