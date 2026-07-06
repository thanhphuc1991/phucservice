package com.phuchoccode.CommonnService.common.event;

public record PaymentProcessedEvent(
        String paymentId,
        String orderId,
        boolean success,
        String failureReason    // null nếu success
) implements SagaEvent {
    // Static factory methods — tạo object rõ ràng hơn
    public static PaymentProcessedEvent success(String paymentId, String orderId) {
        return new PaymentProcessedEvent(paymentId, orderId, true, null);
    }

    public static PaymentProcessedEvent failure(String paymentId, String orderId, String reason) {
        return new PaymentProcessedEvent(paymentId, orderId, false, reason);
    }
}