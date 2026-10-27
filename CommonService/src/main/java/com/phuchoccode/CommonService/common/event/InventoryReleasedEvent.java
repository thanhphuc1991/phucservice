package com.phuchoccode.CommonService.common.event;

public record InventoryReleasedEvent (
        String eventId,
        String orderId,
        String productId,
        int quantity
) implements SagaEvent {
}
