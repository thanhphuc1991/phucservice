// common/event/shipment/ShipmentStartedEvent.java
package com.phuchoccode.CommonnService.common.event;

public record ShipmentStartedEvent(
        String shipmentId,
        String orderId,
        boolean success,
        String failureReason
) implements SagaEvent {
    public static ShipmentStartedEvent success(String shipmentId, String orderId) {
        return new ShipmentStartedEvent(shipmentId, orderId, true, null);
    }

    public static ShipmentStartedEvent failure(String shipmentId, String orderId, String reason) {
        return new ShipmentStartedEvent(shipmentId, orderId, false, reason);
    }
}