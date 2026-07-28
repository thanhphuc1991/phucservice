// domain/aggregate/OrderAggregate.java
package com.phuchoccode.OrderService.domain.aggregate;

import com.phuchoccode.OrderService.domain.entity.OrderItem;
import com.phuchoccode.OrderService.domain.event.OrderDomainEvent;
import com.phuchoccode.OrderService.domain.event.OrderCancelledDomainEvent;
import com.phuchoccode.OrderService.domain.event.OrderCompletedEvent;
import com.phuchoccode.OrderService.domain.event.OrderPlacedEvent;
import com.phuchoccode.OrderService.domain.exception.OrderDomainException;
import com.phuchoccode.OrderService.domain.valueobject.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderAggregate {

    // Identity
    private final OrderId orderId;
    private final CustomerId customerId;

    // State
    private OrderStatus status;
    private final List<OrderItem> items;

    // Domain events chờ publish
    private final List<OrderDomainEvent> domainEvents = new ArrayList<>();

    // ─── Constructor — chỉ tạo qua factory method ────────────

    private OrderAggregate(OrderId orderId, CustomerId customerId,
                           List<OrderItem> items) {
        this.orderId    = orderId;
        this.customerId = customerId;
        this.items      = new ArrayList<>(items);
        this.status     = OrderStatus.PENDING;
    }

    // ─── Factory method ───────────────────────────────────────

    public static OrderAggregate create(CustomerId customerId,
                                        List<OrderItem> items) {
        if (items == null || items.isEmpty())
            throw new OrderDomainException("Order must have at least one item");

        var orderId    = OrderId.generate();
        var aggregate  = new OrderAggregate(orderId, customerId, items);

        // Raise domain event
        aggregate.raise(OrderPlacedEvent.of(
                orderId.value(),
                customerId.value(),
                items.get(0).productId().value(),
                items.get(0).quantity(),
                aggregate.totalAmount().amount().doubleValue()
        ));

        return aggregate;
    }

    public static OrderAggregate restore(OrderId orderId, CustomerId customerId,
                                         List<OrderItem> items, OrderStatus status) {
        var aggregate  = new OrderAggregate(orderId, customerId, items);
        aggregate.status = status;
        return aggregate;   // không raise event vì đây là load từ DB
    }

    // ─── Business logic ───────────────────────────────────────

    public void startInventoryReserving() {
        validateStatus(OrderStatus.PENDING, "Cannot reserve inventory");
        this.status = OrderStatus.INVENTORY_RESERVING;
    }

    public void startPaymentProcessing() {
        validateStatus(OrderStatus.INVENTORY_RESERVING, "Cannot process payment");
        this.status = OrderStatus.PAYMENT_PROCESSING;
    }

    public void startShipmentProcessing() {
        validateStatus(OrderStatus.PAYMENT_PROCESSING, "Cannot start shipment");
        this.status = OrderStatus.SHIPMENT_PROCESSING;
    }

    public void complete() {
        validateStatus(OrderStatus.SHIPMENT_PROCESSING, "Cannot complete order");
        this.status = OrderStatus.COMPLETED;
        raise(OrderCompletedEvent.of(orderId.value()));
    }

    public void cancel(String reason, String failedStep) {
        if (status == OrderStatus.COMPLETED)
            throw new OrderDomainException("Cannot cancel a completed order");

        this.status = OrderStatus.CANCELLED;
        raise(OrderCancelledDomainEvent.of(orderId.value(), reason, failedStep));
    }

    // ─── Calculated value ─────────────────────────────────────

    public Money totalAmount() {
        return items.stream()
                .map(OrderItem::subTotal)
                .reduce(Money.ZERO, Money::add);
    }

    // ─── Domain events ────────────────────────────────────────

    private void raise(OrderDomainEvent event) {
        domainEvents.add(event);
    }

    public List<OrderDomainEvent> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();   // clear sau khi lấy ra
        return events;
    }

    // ─── Private helper ───────────────────────────────────────

    private void validateStatus(OrderStatus expected, String message) {
        if (this.status != expected)
            throw new OrderDomainException(
                    message + " — current status: " + this.status
            );
    }

    // ─── Getters ──────────────────────────────────────────────

    public OrderId orderId()         { return orderId; }
    public CustomerId customerId()   { return customerId; }
    public OrderStatus status()      { return status; }
    public List<OrderItem> items()   { return Collections.unmodifiableList(items); }
}