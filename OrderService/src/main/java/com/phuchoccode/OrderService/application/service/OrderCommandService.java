package com.phuchoccode.OrderService.application.service;

import com.phuchoccode.CommonnService.common.event.*;
import com.phuchoccode.OrderService.domain.aggregate.OrderAggregate;
import com.phuchoccode.OrderService.domain.entity.OrderItem;
import com.phuchoccode.OrderService.domain.repository.OrderRepository;
import com.phuchoccode.OrderService.domain.valueobject.*;
import com.phuchoccode.OrderService.infrastructure.messaging.KafkaEventPublisher;
import com.phuchoccode.OrderService.request.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCommandService {

    private final OrderRepository orderRepository;     // ← domain port, không phải JpaOrderRepository
    private final KafkaEventPublisher eventPublisher;

    public OrderAggregate createOrder(CreateOrderRequest request) {
        List<String> violations = Stream.of(
                        request.customerId() == null || request.customerId().isBlank()
                                ? "customerId required" : null,
                        request.productId() == null || request.productId().isBlank()
                                ? "productId required" : null,
                        request.quantity() <= 0
                                ? "quantity must > 0" : null,
                        request.totalAmount() <= 0
                                ? "amount must > 0" : null
                )
                .filter(Objects::nonNull)
                .toList();

        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", violations));
        }

        var customerId = CustomerId.of(request.customerId());
        var productId  = ProductId.of(request.productId());
        var unitPrice  = Money.of(request.totalAmount() / request.quantity());
        var item       = new OrderItem(productId, request.quantity(), unitPrice);
        var order      = OrderAggregate.create(customerId, List.of(item));
        order.startInventoryReserving();

        orderRepository.save(order);
        eventPublisher.publish(order.pullDomainEvents());

        log.info("Saga started — orderId={}", order.orderId().value());
        return order;
    }

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

    private void handleEvent(SagaEvent event) {
        switch (event) {

            case InventoryReservedEvent e when !e.reserved() ->
                    cancelOrder(e.orderId(), "INVENTORY", "Inventory unavailable");

            case InventoryReservedEvent e ->
                // Chỉ log — OrderService không cần update DB ở bước này
                    log.info("Inventory reserved OK — orderId={}", e.orderId());

            case PaymentProcessedEvent e when !e.success() ->
                    cancelOrder(e.orderId(), "PAYMENT", e.failureReason());

            case PaymentProcessedEvent e ->
                // Chỉ log — chờ ShipmentService xử lý tiếp
                    log.info("Payment processed OK — orderId={}", e.orderId());

            case ShipmentStartedEvent e when !e.success() ->
                    cancelOrder(e.orderId(), "SHIPMENT", e.failureReason());

            case ShipmentStartedEvent e -> {
                orderRepository.findById(OrderId.of(e.orderId()))
                        .ifPresent(order -> {
                            order.complete();
                            orderRepository.save(order);
                            eventPublisher.publish(order.pullDomainEvents());
                            log.info("Saga COMPLETED — orderId={}", e.orderId());
                        });
            }

            case OrderCreatedEvent e ->
                    log.debug("OrderCreatedEvent — orderId={}", e.orderId());

            case OrderCancelledEvent e ->
                    log.warn("OrderCancelledEvent — orderId={}", e.orderId());

            case PaymentRefundedEvent e ->
                    log.info("PaymentRefunded — orderId={}", e.orderId());

            case InventoryReleasedEvent e ->
                    log.info("InventoryReleased — orderId={}", e.orderId());
        }
    }

    private void cancelOrder(String orderId, String failedStep, String reason) {
        orderRepository.findById(OrderId.of(orderId))
                .filter(o -> o.status() != OrderStatus.CANCELLED)
                .ifPresent(order -> {
                    order.cancel(reason, failedStep);
                    orderRepository.save(order);
                    eventPublisher.publish(order.pullDomainEvents());
                    log.warn("Saga CANCELLED — orderId={}, step={}", orderId, failedStep);
                });
    }
}