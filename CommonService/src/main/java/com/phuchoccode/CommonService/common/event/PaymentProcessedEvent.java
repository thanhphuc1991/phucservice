package com.phuchoccode.CommonService.common.event;

import java.util.UUID;

public record PaymentProcessedEvent(
        String eventId,
        String paymentId,
        String orderId,
        boolean success,
        String failureReason    // null nếu success
) implements SagaEvent {
    // Static factory methods — tạo object rõ ràng hơn
    public static PaymentProcessedEvent success(String paymentId, String orderId) {
        return new PaymentProcessedEvent(UUID.randomUUID().toString(), paymentId, orderId, true, null);
    }

    public static PaymentProcessedEvent failure(String paymentId, String orderId, String reason) {
        return new PaymentProcessedEvent(UUID.randomUUID().toString(), paymentId, orderId, false, reason);
    }
}