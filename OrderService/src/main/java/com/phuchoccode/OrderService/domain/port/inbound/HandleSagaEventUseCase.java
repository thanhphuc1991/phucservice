package com.phuchoccode.OrderService.domain.port.inbound;

import com.phuchoccode.CommonService.common.event.*;

public interface HandleSagaEventUseCase {
    void onInventoryReserved(InventoryReservedEvent event);
    void onPaymentProcessed(PaymentProcessedEvent event);
    void onPaymentFailed(PaymentProcessedEvent event);
    void onShipmentStarted(ShipmentStartedEvent event);
    void onShipmentFailed(ShipmentStartedEvent event);
}
