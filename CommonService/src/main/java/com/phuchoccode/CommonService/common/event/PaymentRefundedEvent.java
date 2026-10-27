package com.phuchoccode.CommonService.common.event;

public record PaymentRefundedEvent(
        String eventId,
        String orderId,
        String paymentId
) implements SagaEvent {
}
