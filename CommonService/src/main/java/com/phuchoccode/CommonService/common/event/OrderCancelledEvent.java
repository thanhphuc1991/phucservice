package com.phuchoccode.CommonService.common.event;

public record OrderCancelledEvent(
        String eventId,
        String orderId,
        String reason,
        String failedStep// "INVENTORY" | "PAYMENT" | "SHIPMENT"

) implements SagaEvent {
}
