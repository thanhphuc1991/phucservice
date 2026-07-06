package com.phuchoccode.OrderService.service;

import com.phuchoccode.CommonnService.common.event.*;
import com.phuchoccode.OrderService.entity.Order;
import com.phuchoccode.OrderService.model.OrderStatus;
import com.phuchoccode.OrderService.repository.OrderRepository;
import com.phuchoccode.OrderService.request.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ─── Tạo order và bắt đầu Saga ──────────────────────────

    public Order createOrder(CreateOrderRequest request) {
        List<String> violations = Stream.of(
                        request.customerId() == null ? "customerId required" : null,
                        request.productId()  == null ? "productId required"  : null,
                        request.quantity()   <= 0    ? "quantity must > 0"   : null,
                        request.totalAmount()<= 0    ? "amount must > 0"     : null
                )
                .filter(Objects::nonNull)
                .toList();

        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", violations));
        }

        var order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setCustomerId(request.customerId());
        order.setProductId(request.productId());
        order.setQuantity(request.quantity());
        order.setTotalAmount(request.totalAmount());
        order.setStatus(OrderStatus.INVENTORY_RESERVING);
        orderRepository.save(order);

        var event = new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalAmount()
        );

        kafkaTemplate.send("order.created", order.getId(), event);
        log.info("Saga started — orderId={}", order.getId());

        return order;
    }

    // ─── Nhận events và dùng Pattern Matching ────────────────

    @KafkaListener(topics = "inventory.reserved", groupId = "order-service-group")
    public void onInventoryReserved(InventoryReservedEvent event) {
        handleEvent(event);
    }

    @KafkaListener(topics = "payment.processed", groupId = "order-service-group")
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        handleEvent(event);
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-service-group")
    public void onPaymentFailed(PaymentProcessedEvent event) {
        handleEvent(event);
    }

    @KafkaListener(topics = "shipment.started", groupId = "order-service-group")
    public void onShipmentStarted(ShipmentStartedEvent event) {
        handleEvent(event);
    }

    @KafkaListener(topics = "shipment.failed", groupId = "order-service-group")
    public void onShipmentFailed(ShipmentStartedEvent event) {
        handleEvent(event);
    }

    // ─── Pattern Matching — Java 21+ ─────────────────────────

    private void handleEvent(SagaEvent event) {
        switch (event) {

            case InventoryReservedEvent e when !e.reserved() ->
                    handleSagaFailed(e.orderId(), "INVENTORY");

            case InventoryReservedEvent e ->
                    log.info("Inventory reserved OK — orderId={}", e.orderId());

            case PaymentProcessedEvent e when !e.success() ->
                    handleSagaFailed(e.orderId(), "PAYMENT");

            case PaymentProcessedEvent e ->
                    log.info("Payment processed OK — orderId={}", e.orderId());

            case ShipmentStartedEvent e when !e.success() ->
                    handleSagaFailed(e.orderId(), "SHIPMENT");

            case ShipmentStartedEvent e -> {
                orderRepository.findById(e.orderId())
                        .ifPresentOrElse(
                                order -> {
                                    order.setStatus(OrderStatus.COMPLETED);
                                    orderRepository.save(order);
                                    log.info("Saga COMPLETED — orderId={}", e.orderId());
                                },
                                () -> log.warn("Order not found — orderId={}", e.orderId())
                        );
            }

            case OrderCreatedEvent e ->
                    log.debug("OrderCreatedEvent received — orderId={}", e.orderId());

            case OrderCancelledEvent e ->
                    log.warn("OrderCancelledEvent received — orderId={}", e.orderId());

            case PaymentRefundedEvent e ->
                    log.info("PaymentRefundedEvent received — orderId={}", e.orderId());

            // ← Thêm case còn thiếu
            case InventoryReleasedEvent e ->
                    log.info("Inventory released — orderId={}", e.orderId());
        }
    }

    // ─── Private helper ──────────────────────────────────────

    private void handleSagaFailed(String orderId, String failedStep) {
        orderRepository.findById(orderId)
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .ifPresent(order -> {
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);

                    kafkaTemplate.send("order.cancelled", orderId,
                            new OrderCancelledEvent(
                                    orderId,
                                    "Failed at: " + failedStep,
                                    failedStep
                            )
                    );
                    log.warn("Saga CANCELLED — orderId={}, step={}", orderId, failedStep);
                });
    }
}