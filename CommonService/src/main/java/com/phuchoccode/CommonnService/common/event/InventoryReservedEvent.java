package com.phuchoccode.CommonnService.common.event;

public record InventoryReservedEvent(
        String orderId,
        String productId,
        int quantity,
        boolean reserved,
        String failureReason
) implements SagaEvent {
    public static InventoryReservedEvent success(String orderId, String productId, int qty) {
        return new InventoryReservedEvent(orderId, productId, qty, true, null);
    }

    public static InventoryReservedEvent failure(String orderId, String productId, int qty, String reason) {
        return new InventoryReservedEvent(orderId, productId, qty, false, reason);
    }
}
