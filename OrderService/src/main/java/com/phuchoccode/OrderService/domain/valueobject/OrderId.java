// domain/valueobject/OrderId.java
package com.phuchoccode.OrderService.domain.valueobject;

import java.util.UUID;

public record OrderId(String value) {

    public OrderId {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("OrderId cannot be blank");
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID().toString());
    }

    public static OrderId of(String value) {
        return new OrderId(value);
    }
}