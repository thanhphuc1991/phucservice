package com.phuchoccode.OrderService.model;

public enum OrderStatus {
    CREATED,
    INVENTORY_RESERVING,
    PAYMENT_PROCESSING,
    SHIPMENT_PROCESSING,
    COMPLETED,
    CANCELLED
}
