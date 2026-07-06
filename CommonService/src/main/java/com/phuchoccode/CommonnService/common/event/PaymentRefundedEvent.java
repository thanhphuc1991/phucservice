package com.phuchoccode.CommonnService.common.event;

public record PaymentRefundedEvent(
        String orderId,
        String paymentId
) implements SagaEvent {
}
