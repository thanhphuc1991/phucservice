package com.phuchoccode.OrderService.application.service;

import com.phuchoccode.CommonService.common.event.*;
import com.phuchoccode.OrderService.domain.aggregate.OrderAggregate;
import com.phuchoccode.OrderService.domain.entity.OrderItem;
import com.phuchoccode.OrderService.domain.exception.IllegalStateTransitionException;
import com.phuchoccode.OrderService.domain.port.inbound.CreateOrderUseCase;
import com.phuchoccode.OrderService.domain.port.inbound.HandleSagaEventUseCase;
import com.phuchoccode.OrderService.domain.port.outbound.OrderEventPublisherPort;
import com.phuchoccode.OrderService.domain.port.outbound.OrderRepositoryPort;
import com.phuchoccode.OrderService.domain.valueobject.*;
import com.phuchoccode.OrderService.request.CreateOrderRequest;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCommandService implements CreateOrderUseCase, HandleSagaEventUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OrderEventPublisherPort eventPublisher;
    private final ObservationRegistry observationRegistry;

    // ─── CreateOrderUseCase ───────────────────────────────────

    @Override
    @Transactional
    public OrderAggregate createOrder(CreateOrderRequest request) {

        // Tạo custom span để track thời gian tạo order
        return Observation.createNotStarted("order.create", observationRegistry)
                .lowCardinalityKeyValue("customerId", request.customerId())
                .observe(() -> {
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
                });
    }

    @Override
    public Optional<OrderAggregate> findById(String orderId) {
        return orderRepository.findById(OrderId.of(orderId));
    }

    // ─── HandleSagaEventUseCase ───────────────────────────────

    @Override
    @KafkaListener(topics = "inventory.reserved", groupId = "order-service-group")
    public void onInventoryReserved(InventoryReservedEvent event) {
        handleEvent(event);
    }

    @Override
    @KafkaListener(topics = "payment.processed", groupId = "order-service-group")
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        handleEvent(event);
    }

    @Override
    @KafkaListener(topics = "payment.failed", groupId = "order-service-group")
    public void onPaymentFailed(PaymentProcessedEvent event) {
        handleEvent(event);
    }

    @Override
    @KafkaListener(topics = "shipment.started", groupId = "order-service-group")
    public void onShipmentStarted(ShipmentStartedEvent event) {
        handleEvent(event);
    }

    @Override
    @KafkaListener(topics = "shipment.failed", groupId = "order-service-group")
    public void onShipmentFailed(ShipmentStartedEvent event) {
        handleEvent(event);
    }

    // ─── Pattern matching ─────────────────────────────────────

    private void handleEvent(SagaEvent event) {
        try {
            switch (event) {
                case InventoryReservedEvent e when !e.reserved() ->
                        cancelOrder(e.orderId(), "INVENTORY", "Inventory unavailable");

                case InventoryReservedEvent e ->
                        log.info("Inventory reserved OK — orderId={}", e.orderId());

                case PaymentProcessedEvent e when !e.success() ->
                        cancelOrder(e.orderId(), "PAYMENT", e.failureReason());

                case PaymentProcessedEvent e ->
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
        } catch (IllegalStateTransitionException ex) {
            // Event đến trễ hoặc duplicate — log và bỏ qua
            log.warn("Invalid state transition — skipping event: {}, reason: {}",
                    event.getClass().getSimpleName(), ex.getMessage());
        }
    }

    // ─── Private helper ───────────────────────────────────────

    @Transactional    // ← cancel + outbox trong cùng 1 transaction
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