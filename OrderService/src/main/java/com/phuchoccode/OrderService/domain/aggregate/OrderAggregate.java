package com.phuchoccode.OrderService.domain.aggregate;

import com.phuchoccode.OrderService.domain.entity.OrderItem;
import com.phuchoccode.OrderService.domain.event.*;
import com.phuchoccode.OrderService.domain.exception.OrderDomainException;
import com.phuchoccode.OrderService.domain.statemachine.OrderStatusTransition;
import com.phuchoccode.OrderService.domain.valueobject.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderAggregate {

    private final OrderId orderId;
    private final CustomerId customerId;
    private OrderStatus status;
    private final List<OrderItem> items;
    private final List<OrderDomainEvent> domainEvents = new ArrayList<>();

    private OrderAggregate(OrderId orderId, CustomerId customerId, List<OrderItem> items) {
        this.orderId    = orderId;
        this.customerId = customerId;
        this.items      = new ArrayList<>(items);
        this.status     = OrderStatus.PENDING;
    }

    // ─── Factory methods ──────────────────────────────────────

    public static OrderAggregate create(CustomerId customerId, List<OrderItem> items) {
        if (items == null || items.isEmpty())
            throw new OrderDomainException("Order must have at least one item");

        var orderId   = OrderId.generate();
        var aggregate = new OrderAggregate(orderId, customerId, items);

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
        var aggregate = new OrderAggregate(orderId, customerId, items);
        aggregate.status = status;
        return aggregate;
    }

    // ─── Business logic — dùng State Machine ─────────────────

    public void startInventoryReserving() {
        transitionTo(OrderStatus.INVENTORY_RESERVING);
    }

    public void startPaymentProcessing() {
        transitionTo(OrderStatus.PAYMENT_PROCESSING);
    }

    public void startShipmentProcessing() {
        transitionTo(OrderStatus.SHIPMENT_PROCESSING);
    }

    public void complete() {
        transitionTo(OrderStatus.COMPLETED);
        raise(OrderCompletedEvent.of(orderId.value()));
    }

    public void cancel(String reason, String failedStep) {
        transitionTo(OrderStatus.CANCELLED);
        raise(OrderCancelledDomainEvent.of(orderId.value(), reason, failedStep));
    }

    // ─── Core State Machine ───────────────────────────────────

    private void transitionTo(OrderStatus newStatus) {
        // Delegate hoàn toàn cho State Machine
        OrderStatusTransition.validate(this.status, newStatus);
        log(this.status, newStatus);
        this.status = newStatus;
    }

    private void log(OrderStatus from, OrderStatus to) {
        System.out.printf("[OrderAggregate] %s → %s (orderId=%s)%n",
                from, to, orderId.value());
    }

    // ─── Domain events ────────────────────────────────────────

    private void raise(OrderDomainEvent event) {
        domainEvents.add(event);
    }

    public List<OrderDomainEvent> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    // ─── Calculated value ─────────────────────────────────────

    public Money totalAmount() {
        return items.stream()
                .map(OrderItem::subTotal)
                .reduce(Money.ZERO, Money::add);
    }

    // ─── Getters ──────────────────────────────────────────────

    public OrderId orderId()       { return orderId; }
    public CustomerId customerId() { return customerId; }
    public OrderStatus status()    { return status; }
    public List<OrderItem> items() { return Collections.unmodifiableList(items); }
}