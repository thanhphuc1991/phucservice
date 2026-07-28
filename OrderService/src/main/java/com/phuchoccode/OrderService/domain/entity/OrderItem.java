// domain/entity/OrderItem.java
package com.phuchoccode.OrderService.domain.entity;

import com.phuchoccode.OrderService.domain.valueobject.Money;
import com.phuchoccode.OrderService.domain.valueobject.ProductId;

public class OrderItem {

    private final ProductId productId;
    private int quantity;
    private final Money unitPrice;

    public OrderItem(ProductId productId, int quantity, Money unitPrice) {
        if (quantity <= 0)
            throw new IllegalArgumentException("Quantity must be > 0");

        this.productId = productId;
        this.quantity  = quantity;
        this.unitPrice = unitPrice;
    }

    // Business logic trong Entity
    public Money subTotal() {
        return unitPrice.multiply(quantity);
    }

    public void updateQuantity(int newQuantity) {
        if (newQuantity <= 0)
            throw new IllegalArgumentException("Quantity must be > 0");
        this.quantity = newQuantity;
    }

    public ProductId productId() { return productId; }
    public int quantity()        { return quantity; }
    public Money unitPrice()     { return unitPrice; }
}