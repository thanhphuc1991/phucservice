// domain/valueobject/ProductId.java
package com.phuchoccode.OrderService.domain.valueobject;

public record ProductId(String value) {
    public ProductId {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("ProductId cannot be blank");
    }

    public static ProductId of(String value) {
        return new ProductId(value);
    }
}