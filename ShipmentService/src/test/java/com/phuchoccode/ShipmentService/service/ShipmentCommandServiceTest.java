package com.phuchoccode.ShipmentService.service;

import com.phuchoccode.CommonService.common.event.PaymentProcessedEvent;
import com.phuchoccode.CommonService.common.event.ShipmentStartedEvent;
import com.phuchoccode.ShipmentService.entity.Shipment;
import com.phuchoccode.ShipmentService.model.ShipmentStatus;
import com.phuchoccode.ShipmentService.repository.ShipmentRepository;
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
@DisplayName("ShipmentCommandService Unit Tests")
class ShipmentCommandServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ShipmentCommandService shipmentCommandService;

    private static final String ORDER_ID    = "order-001";
    private static final String SHIPMENT_ID = "ship-001";

    // ─── onPaymentProcessed ───────────────────────────────────

    @Nested
    @DisplayName("onPaymentProcessed()")
    class OnPaymentProcessed {

        @Test
        @DisplayName("SUCCESS — payment success, shipment arranged, publishes shipment.started(true)")
        void onPaymentProcessed_shipmentSuccess() {
            var event = PaymentProcessedEvent.success("pay-001", ORDER_ID);
            when(shipmentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            shipmentCommandService.onPaymentProcessed(event);

            ArgumentCaptor<Shipment> shipmentCaptor = ArgumentCaptor.forClass(Shipment.class);
            verify(shipmentRepository).save(shipmentCaptor.capture());
            assertThat(shipmentCaptor.getValue().getStatus()).isEqualTo(ShipmentStatus.STARTED);
            assertThat(shipmentCaptor.getValue().getOrderId()).isEqualTo(ORDER_ID);

            ArgumentCaptor<ShipmentStartedEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentStartedEvent.class);
            verify(kafkaTemplate).send(eq("shipment.started"), eq(ORDER_ID), eventCaptor.capture());
            assertThat(eventCaptor.getValue().success()).isTrue();
            assertThat(eventCaptor.getValue().orderId()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("SKIP — payment not success (success=false), skips shipment entirely")
        void onPaymentProcessed_paymentFailed_skips() {
            var event = PaymentProcessedEvent.failure("pay-001", ORDER_ID, "Insufficient balance");

            shipmentCommandService.onPaymentProcessed(event);

            verifyNoInteractions(shipmentRepository, kafkaTemplate);
        }

        @Test
        @DisplayName("IDEMPOTENCY — shipment already processed, skips duplicate")
        void onPaymentProcessed_alreadyProcessed_skips() {
            var event = PaymentProcessedEvent.success("pay-001", ORDER_ID);
            var existingShipment = buildShipment(SHIPMENT_ID, ORDER_ID, ShipmentStatus.STARTED);

            when(shipmentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existingShipment));

            shipmentCommandService.onPaymentProcessed(event);

            verify(shipmentRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("FAILURE — arrangeShipment fails, saves FAILED shipment and publishes shipment.failed")
        void onPaymentProcessed_arrangeShipmentFails_publishesFailed() {
            // Use a spy to override arrangeShipment to return false
            var realService = new ShipmentCommandService(shipmentRepository, kafkaTemplate) {
                @Override
                protected boolean arrangeShipment(String orderId) {
                    return false;
                }
            };

            var event = PaymentProcessedEvent.success("pay-001", ORDER_ID);
            when(shipmentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            realService.onPaymentProcessed(event);

            ArgumentCaptor<Shipment> shipmentCaptor = ArgumentCaptor.forClass(Shipment.class);
            verify(shipmentRepository).save(shipmentCaptor.capture());
            assertThat(shipmentCaptor.getValue().getStatus()).isEqualTo(ShipmentStatus.FAILED);
            assertThat(shipmentCaptor.getValue().getFailureReason()).isEqualTo("No carrier available");

            ArgumentCaptor<ShipmentStartedEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentStartedEvent.class);
            verify(kafkaTemplate).send(eq("shipment.failed"), eq(ORDER_ID), eventCaptor.capture());
            assertThat(eventCaptor.getValue().success()).isFalse();
            assertThat(eventCaptor.getValue().failureReason()).isEqualTo("No carrier available");
        }
    }

    // ─── Helper ──────────────────────────────────────────────

    private Shipment buildShipment(String id, String orderId, ShipmentStatus status) {
        var shipment = new Shipment();
        shipment.setId(id);
        shipment.setOrderId(orderId);
        shipment.setStatus(status);
        return shipment;
    }
}
