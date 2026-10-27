package com.phuchoccode.CommonService.common.event;

import java.util.UUID;

public record InventoryReservedEvent(
        String eventId,
        String orderId,
        String productId,
        int quantity,
        boolean reserved,
        String failureReason
) implements SagaEvent {
    public static InventoryReservedEvent success(String orderId, String productId, int qty) {
        return new InventoryReservedEvent(UUID.randomUUID().toString(), orderId, productId, qty, true, null);
    }

    public static InventoryReservedEvent failure(String orderId, String productId, int qty, String reason) {
        return new InventoryReservedEvent(UUID.randomUUID().toString(), orderId, productId, qty, false, reason);
    }
}
