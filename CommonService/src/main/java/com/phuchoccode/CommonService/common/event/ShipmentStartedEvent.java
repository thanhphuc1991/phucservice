// common/event/shipment/ShipmentStartedEvent.java
package com.phuchoccode.CommonService.common.event;

import java.util.UUID;

public record ShipmentStartedEvent(
        String eventId,
        String shipmentId,
        String orderId,
        boolean success,
        String failureReason
) implements SagaEvent {
    public static ShipmentStartedEvent success(String shipmentId, String orderId) {
        return new ShipmentStartedEvent(UUID.randomUUID().toString(), shipmentId, orderId, true, null);
    }

    public static ShipmentStartedEvent failure(String shipmentId, String orderId, String reason) {
        return new ShipmentStartedEvent(UUID.randomUUID().toString(), shipmentId, orderId, false, reason);
    }
}