package com.phuchoccode.OrderService.service;

import com.phuchoccode.CommonService.common.event.InventoryReservedEvent;
import com.phuchoccode.CommonService.common.event.PaymentProcessedEvent;
import com.phuchoccode.CommonService.common.event.ShipmentStartedEvent;
import com.phuchoccode.OrderService.application.service.OrderCommandService;
import com.phuchoccode.OrderService.domain.aggregate.OrderAggregate;
import com.phuchoccode.OrderService.domain.exception.OrderDomainException;
import com.phuchoccode.OrderService.domain.port.outbound.OrderEventPublisherPort;
import com.phuchoccode.OrderService.domain.port.outbound.OrderRepositoryPort;
import com.phuchoccode.OrderService.domain.repository.OrderRepository;
import com.phuchoccode.OrderService.domain.valueobject.CustomerId;
import com.phuchoccode.OrderService.domain.valueobject.Money;
import com.phuchoccode.OrderService.domain.valueobject.OrderId;
import com.phuchoccode.OrderService.domain.valueobject.OrderStatus;
import com.phuchoccode.OrderService.domain.valueobject.ProductId;
import com.phuchoccode.OrderService.domain.entity.OrderItem;
import com.phuchoccode.OrderService.infrastructure.messaging.KafkaEventPublisher;
import com.phuchoccode.OrderService.request.CreateOrderRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCommandService Unit Tests")
class OrderCommandServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private OrderEventPublisherPort eventPublisher;

    @InjectMocks
    private OrderCommandService orderCommandService;

    private static final String ORDER_ID    = "order-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final String PRODUCT_ID  = "prod-001";

    @Nested
    @DisplayName("createOrder()")
    class CreateOrder {

        @Test
        @DisplayName("SUCCESS — valid request saves aggregate and publishes events")
        void createOrder_success() {
            var request = new CreateOrderRequest(CUSTOMER_ID, PRODUCT_ID, 2, 100.0);
            when(orderRepository.save(any(OrderAggregate.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrderAggregate result = orderCommandService.createOrder(request);

            assertThat(result.status()).isEqualTo(OrderStatus.INVENTORY_RESERVING);
            assertThat(result.customerId().value()).isEqualTo(CUSTOMER_ID);
            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).productId().value()).isEqualTo(PRODUCT_ID);
            assertThat(result.items().get(0).quantity()).isEqualTo(2);

            verify(orderRepository).save(any(OrderAggregate.class));
            verify(eventPublisher).publish(anyList());
        }

        @Test
        @DisplayName("FAILURE — null customerId throws IllegalArgumentException")
        void createOrder_nullCustomerId_throws() {
            var request = new CreateOrderRequest(null, PRODUCT_ID, 2, 100.0);
            assertThatThrownBy(() -> orderCommandService.createOrder(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("customerId required");
        }

        @Test
        @DisplayName("FAILURE — null productId throws IllegalArgumentException")
        void createOrder_nullProductId_throws() {
            var request = new CreateOrderRequest(CUSTOMER_ID, null, 2, 100.0);
            assertThatThrownBy(() -> orderCommandService.createOrder(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("productId required");
        }

        @Test
        @DisplayName("FAILURE — quantity <= 0 throws IllegalArgumentException")
        void createOrder_zeroQuantity_throws() {
            var request = new CreateOrderRequest(CUSTOMER_ID, PRODUCT_ID, 0, 100.0);
            assertThatThrownBy(() -> orderCommandService.createOrder(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must > 0");
        }

        @Test
        @DisplayName("FAILURE — totalAmount <= 0 throws IllegalArgumentException")
        void createOrder_zeroAmount_throws() {
            var request = new CreateOrderRequest(CUSTOMER_ID, PRODUCT_ID, 2, 0.0);
            assertThatThrownBy(() -> orderCommandService.createOrder(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount must > 0");
        }

        @Test
        @DisplayName("FAILURE — multiple violations reported together")
        void createOrder_multipleViolations_throws() {
            var request = new CreateOrderRequest(null, null, 0, 0.0);
            assertThatThrownBy(() -> orderCommandService.createOrder(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("customerId required")
                    .hasMessageContaining("productId required");
        }
    }

    @Nested
    @DisplayName("onInventoryReserved()")
    class OnInventoryReserved {

        @Test
        @DisplayName("SUCCESS — reserved=true logs OK, no DB update")
        void onInventoryReserved_success_noDbChange() {
            var event = InventoryReservedEvent.success(ORDER_ID, PRODUCT_ID, 2);

            orderCommandService.onInventoryReserved(event);

            // reserved=true chỉ log, không tương tác DB hay Kafka
            verifyNoInteractions(orderRepository, eventPublisher);
        }

        @Test
        @DisplayName("FAILURE — reserved=false cancels order and publishes events")
        void onInventoryReserved_failure_cancelsOrder() {
            var event     = InventoryReservedEvent.failure(ORDER_ID, PRODUCT_ID, 2, "Insufficient stock");
            var aggregate = buildAggregate(ORDER_ID, OrderStatus.INVENTORY_RESERVING);

            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(aggregate));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            orderCommandService.onInventoryReserved(event);

            assertThat(aggregate.status()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(aggregate);
            verify(eventPublisher).publish(anyList());
        }
    }

    @Nested
    @DisplayName("onPaymentProcessed()")
    class OnPaymentProcessed {

        @Test
        @DisplayName("SUCCESS — payment success logs OK, no DB update")
        void onPaymentProcessed_success_noDbChange() {
            var event = PaymentProcessedEvent.success("pay-001", ORDER_ID);

            orderCommandService.onPaymentProcessed(event);

            // success chỉ log, không tương tác DB hay Kafka
            verifyNoInteractions(orderRepository, eventPublisher);
        }

        @Test
        @DisplayName("FAILURE — payment failed cancels order and publishes events")
        void onPaymentFailed_cancelsOrder() {
            var event     = PaymentProcessedEvent.failure("pay-001", ORDER_ID, "Insufficient balance");
            var aggregate = buildAggregate(ORDER_ID, OrderStatus.PAYMENT_PROCESSING);

            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(aggregate));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            orderCommandService.onPaymentFailed(event);

            assertThat(aggregate.status()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(aggregate);
            verify(eventPublisher).publish(anyList());
        }
    }

    @Nested
    @DisplayName("onShipmentStarted()")
    class OnShipmentStarted {

        @Test
        @DisplayName("SUCCESS — shipment success sets order COMPLETED")
        void onShipmentStarted_success_completesOrder() {
            var event     = ShipmentStartedEvent.success("ship-001", ORDER_ID);
            var aggregate = buildAggregate(ORDER_ID, OrderStatus.SHIPMENT_PROCESSING);

            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(aggregate));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            orderCommandService.onShipmentStarted(event);

            assertThat(aggregate.status()).isEqualTo(OrderStatus.COMPLETED);
            verify(orderRepository).save(aggregate);
            verify(eventPublisher).publish(anyList());
        }

        @Test
        @DisplayName("FAILURE — shipment failed cancels order and publishes events")
        void onShipmentFailed_cancelsOrder() {
            var event     = ShipmentStartedEvent.failure("ship-001", ORDER_ID, "No carrier available");
            var aggregate = buildAggregate(ORDER_ID, OrderStatus.SHIPMENT_PROCESSING);

            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(aggregate));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            orderCommandService.onShipmentFailed(event);

            assertThat(aggregate.status()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(aggregate);
            verify(eventPublisher).publish(anyList());
        }

        @Test
        @DisplayName("FAILURE — order not found on shipment success — no save")
        void onShipmentStarted_orderNotFound_noSave() {
            var event = ShipmentStartedEvent.success("ship-001", ORDER_ID);
            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.empty());

            orderCommandService.onShipmentStarted(event);

            verify(orderRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("Idempotency — already CANCELLED")
    class Idempotency {

        @Test
        @DisplayName("handleSagaFailed skips if order already CANCELLED")
        void sagaFailed_alreadyCancelled_skips() {
            var event     = InventoryReservedEvent.failure(ORDER_ID, PRODUCT_ID, 2, "Insufficient stock");
            var aggregate = buildAggregate(ORDER_ID, OrderStatus.CANCELLED);

            when(orderRepository.findById(OrderId.of(ORDER_ID)))
                    .thenReturn(Optional.of(aggregate));

            orderCommandService.onInventoryReserved(event);

            verify(orderRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("OrderAggregate domain logic")
    class AggregateDomainLogic {

        @Test
        @DisplayName("totalAmount = unitPrice × quantity")
        void totalAmount_calculatedCorrectly() {
            var aggregate = buildAggregate(ORDER_ID, OrderStatus.PENDING);
            // unitPrice=50, quantity=2 → totalAmount=100
            assertThat(aggregate.totalAmount().amount().doubleValue()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("cancel COMPLETED order throws OrderDomainException")
        void cancel_completedOrder_throws() {
            var aggregate = buildAggregate(ORDER_ID, OrderStatus.COMPLETED);
            assertThatThrownBy(() -> aggregate.cancel("reason", "STEP"))
                    .isInstanceOf(OrderDomainException.class)
                    .hasMessageContaining("Cannot cancel a completed order");
        }

        @Test
        @DisplayName("startInventoryReserving from wrong status throws")
        void startInventoryReserving_wrongStatus_throws() {
            var aggregate = buildAggregate(ORDER_ID, OrderStatus.PAYMENT_PROCESSING);
            assertThatThrownBy(aggregate::startInventoryReserving)
                    .isInstanceOf(OrderDomainException.class);
        }

        @Test
        @DisplayName("pullDomainEvents clears events after pull")
        void pullDomainEvents_clearsAfterPull() {
            var aggregate = OrderAggregate.create(
                    CustomerId.of(CUSTOMER_ID),
                    List.of(new OrderItem(
                            ProductId.of(PRODUCT_ID),
                            2,
                            Money.of(50.0)
                    ))
            );
            var firstPull  = aggregate.pullDomainEvents();
            var secondPull = aggregate.pullDomainEvents();

            assertThat(firstPull).isNotEmpty();
            assertThat(secondPull).isEmpty();
        }
    }

    // ─── Helper ──────────────────────────────────────────────

    private OrderAggregate buildAggregate(String id, OrderStatus status) {
        return OrderAggregate.restore(
                OrderId.of(id),
                CustomerId.of(CUSTOMER_ID),
                List.of(new OrderItem(
                        ProductId.of(PRODUCT_ID),
                        2,
                        Money.of(50.0)   // unitPrice=50, quantity=2 → totalAmount=100
                )),
                status
        );
    }
}