package com.phuchoccode.CommonnService.common.event;

public record OrderCancelledEvent(
        String orderId,
        String reason,
        String failedStep// "INVENTORY" | "PAYMENT" | "SHIPMENT"

) implements SagaEvent {
}
