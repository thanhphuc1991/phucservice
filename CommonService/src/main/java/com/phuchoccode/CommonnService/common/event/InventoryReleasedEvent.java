package com.phuchoccode.CommonnService.common.event;

public record InventoryReleasedEvent (
        String orderId,
        String productId,
        int quantity
) implements SagaEvent {
}
