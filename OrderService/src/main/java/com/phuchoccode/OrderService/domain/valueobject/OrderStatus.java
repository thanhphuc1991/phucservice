// domain/valueobject/OrderStatus.java
package com.phuchoccode.OrderService.domain.valueobject;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVING,
    PAYMENT_PROCESSING,
    SHIPMENT_PROCESSING,
    COMPLETED,
    CANCELLED
}