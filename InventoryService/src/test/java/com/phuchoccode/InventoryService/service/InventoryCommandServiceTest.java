package com.phuchoccode.InventoryService.service;

import com.phuchoccode.CommonnService.common.event.*;
import com.phuchoccode.InventoryService.entity.Inventory;
import com.phuchoccode.InventoryService.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryCommandService Unit Tests")
class InventoryCommandServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private InventoryCommandService inventoryCommandService;

    private static final String ORDER_ID   = "order-001";
    private static final String PRODUCT_ID = "prod-001";

    // ─── onOrderCreated ───────────────────────────────────────

    @Nested
    @DisplayName("onOrderCreated()")
    class OnOrderCreated {

        @Test
        @DisplayName("SUCCESS — sufficient stock reserves inventory and publishes inventory.reserved(true)")
        void onOrderCreated_sufficientStock_reservesAndPublishes() {
            var event = new OrderCreatedEvent(ORDER_ID, "cust-001", PRODUCT_ID, 3, 150.0);
            var inventory = buildInventory(PRODUCT_ID, 10, 0);

            when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            inventoryCommandService.onOrderCreated(event);

            assertThat(inventory.getAvailableQuantity()).isEqualTo(7);
            assertThat(inventory.getReservedQuantity()).isEqualTo(3);
            verify(inventoryRepository).save(inventory);

            ArgumentCaptor<InventoryReservedEvent> captor = ArgumentCaptor.forClass(InventoryReservedEvent.class);
            verify(kafkaTemplate).send(eq("inventory.reserved"), eq(ORDER_ID), captor.capture());
            assertThat(captor.getValue().reserved()).isTrue();
            assertThat(captor.getValue().quantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("FAILURE — insufficient stock publishes inventory.failed(reserved=false)")
        void onOrderCreated_insufficientStock_publishesFailed() {
            var event = new OrderCreatedEvent(ORDER_ID, "cust-001", PRODUCT_ID, 10, 500.0);
            var inventory = buildInventory(PRODUCT_ID, 5, 0);

            when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(inventory));

            inventoryCommandService.onOrderCreated(event);

            verify(inventoryRepository, never()).save(any());

            ArgumentCaptor<InventoryReservedEvent> captor = ArgumentCaptor.forClass(InventoryReservedEvent.class);
            verify(kafkaTemplate).send(eq("inventory.failed"), eq(ORDER_ID), captor.capture());
            assertThat(captor.getValue().reserved()).isFalse();
            assertThat(captor.getValue().failureReason()).isEqualTo("Insufficient stock");
        }

        @Test
        @DisplayName("FAILURE — product not found publishes inventory.failed")
        void onOrderCreated_productNotFound_publishesFailed() {
            var event = new OrderCreatedEvent(ORDER_ID, "cust-001", PRODUCT_ID, 2, 100.0);
            when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            inventoryCommandService.onOrderCreated(event);

            verify(inventoryRepository, never()).save(any());
            verify(kafkaTemplate).send(eq("inventory.failed"), eq(ORDER_ID), any(InventoryReservedEvent.class));
        }

        @Test
        @DisplayName("EDGE — exact stock match (available == requested) succeeds")
        void onOrderCreated_exactStock_succeeds() {
            var event = new OrderCreatedEvent(ORDER_ID, "cust-001", PRODUCT_ID, 5, 250.0);
            var inventory = buildInventory(PRODUCT_ID, 5, 0);

            when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            inventoryCommandService.onOrderCreated(event);

            assertThat(inventory.getAvailableQuantity()).isEqualTo(0);
            assertThat(inventory.getReservedQuantity()).isEqualTo(5);
            verify(kafkaTemplate).send(eq("inventory.reserved"), eq(ORDER_ID), any(InventoryReservedEvent.class));
        }
    }

    // ─── onPaymentFailed (compensate) ────────────────────────

    @Nested
    @DisplayName("onPaymentFailed() — compensation")
    class OnPaymentFailed {

        @Test
        @DisplayName("SUCCESS — releases reserved stock and publishes inventory.released")
        void onPaymentFailed_releasesStock() {
            var event = PaymentProcessedEvent.failure("pay-001", ORDER_ID, "Insufficient balance");
            var inventory = buildInventory(PRODUCT_ID, 7, 3);
            inventory.setProductId(PRODUCT_ID);

            when(inventoryRepository.findByReservedOrderId(ORDER_ID)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            inventoryCommandService.onPaymentFailed(event);

            // available restored, reserved cleared
            assertThat(inventory.getAvailableQuantity()).isEqualTo(10);
            assertThat(inventory.getReservedQuantity()).isEqualTo(0);
            verify(inventoryRepository).save(inventory);
            verify(kafkaTemplate).send(eq("inventory.released"), eq(ORDER_ID), any(InventoryReleasedEvent.class));
        }

        @Test
        @DisplayName("FAILURE — no reserved inventory found — no action taken")
        void onPaymentFailed_noInventoryFound_noAction() {
            var event = PaymentProcessedEvent.failure("pay-001", ORDER_ID, "Insufficient balance");
            when(inventoryRepository.findByReservedOrderId(ORDER_ID)).thenReturn(Optional.empty());

            inventoryCommandService.onPaymentFailed(event);

            verify(inventoryRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }
    }

    // ─── onShipmentFailed (compensate) ───────────────────────

    @Nested
    @DisplayName("onShipmentFailed() — compensation")
    class OnShipmentFailed {

        @Test
        @DisplayName("SUCCESS — releases reserved stock and publishes inventory.released")
        void onShipmentFailed_releasesStock() {
            var event = ShipmentStartedEvent.failure("ship-001", ORDER_ID, "No carrier available");
            var inventory = buildInventory(PRODUCT_ID, 2, 5);

            when(inventoryRepository.findByReservedOrderId(ORDER_ID)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            inventoryCommandService.onShipmentFailed(event);

            assertThat(inventory.getAvailableQuantity()).isEqualTo(7);
            assertThat(inventory.getReservedQuantity()).isEqualTo(0);
            verify(kafkaTemplate).send(eq("inventory.released"), eq(ORDER_ID), any(InventoryReleasedEvent.class));
        }

        @Test
        @DisplayName("FAILURE — no reserved inventory found — no action taken")
        void onShipmentFailed_noInventoryFound_noAction() {
            var event = ShipmentStartedEvent.failure("ship-001", ORDER_ID, "No carrier available");
            when(inventoryRepository.findByReservedOrderId(ORDER_ID)).thenReturn(Optional.empty());

            inventoryCommandService.onShipmentFailed(event);

            verify(inventoryRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }
    }

    // ─── Helper ──────────────────────────────────────────────

    private Inventory buildInventory(String productId, int available, int reserved) {
        var inv = new Inventory();
        inv.setProductId(productId);
        inv.setAvailableQuantity(available);
        inv.setReservedQuantity(reserved);
        return inv;
    }
}
