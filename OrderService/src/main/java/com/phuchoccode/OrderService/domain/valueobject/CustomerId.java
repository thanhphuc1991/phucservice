// domain/valueobject/CustomerId.java
package com.phuchoccode.OrderService.domain.valueobject;

public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("CustomerId cannot be blank");
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
}