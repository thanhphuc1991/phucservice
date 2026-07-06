package com.phuchoccode.OrderService.request;

public record CreateOrderRequest(
        String customerId,
        String productId,
        int quantity,
        double totalAmount
) {}
