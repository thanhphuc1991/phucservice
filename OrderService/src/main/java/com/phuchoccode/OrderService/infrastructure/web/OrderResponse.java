// infrastructure/web/OrderResponse.java
package com.phuchoccode.OrderService.infrastructure.web;

public record OrderResponse(
        String orderId,
        String customerId,
        String status,
        double totalAmount
) {}