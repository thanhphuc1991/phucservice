package com.phuchoccode.PaymentService.service;

import com.phuchoccode.CommonService.common.event.InventoryReservedEvent;
import com.phuchoccode.CommonService.common.event.PaymentProcessedEvent;
import com.phuchoccode.CommonService.common.event.PaymentRefundedEvent;
import com.phuchoccode.CommonService.common.event.ShipmentStartedEvent;
import com.phuchoccode.PaymentService.entity.Payment;
import com.phuchoccode.PaymentService.model.PaymentStatus;
import com.phuchoccode.PaymentService.repository.PaymentRepository;
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
@DisplayName("PaymentCommandService Unit Tests")
class PaymentCommandServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentCommandService paymentCommandService;

    private static final String ORDER_ID   = "order-001";
    private static final String PAYMENT_ID = "pay-001";

    // ─── onInventoryReserved ──────────────────────────────────

    @Nested
    @DisplayName("onInventoryReserved()")
    class OnInventoryReserved {

        @Test
        @DisplayName("SUCCESS — inventory reserved, payment succeeds, publishes payment.processed(true)")
        void onInventoryReserved_paymentSuccess() {
            var event = InventoryReservedEvent.success(ORDER_ID, "prod-001", 2);
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            paymentCommandService.onInventoryReserved(event);

            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());
            assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(paymentCaptor.getValue().getOrderId()).isEqualTo(ORDER_ID);

            ArgumentCaptor<PaymentProcessedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentProcessedEvent.class);
            verify(kafkaTemplate).send(eq("payment.processed"), eq(ORDER_ID), eventCaptor.capture());
            assertThat(eventCaptor.getValue().success()).isTrue();
        }

        @Test
        @DisplayName("SKIP — inventory not reserved (reserved=false), skips payment entirely")
        void onInventoryReserved_notReserved_skips() {
            var event = InventoryReservedEvent.failure(ORDER_ID, "prod-001", 2, "Insufficient stock");

            paymentCommandService.onInventoryReserved(event);

            verifyNoInteractions(paymentRepository, kafkaTemplate);
        }

        @Test
        @DisplayName("IDEMPOTENCY — payment already processed, skips duplicate processing")
        void onInventoryReserved_alreadyProcessed_skips() {
            var event = InventoryReservedEvent.success(ORDER_ID, "prod-001", 2);
            var existingPayment = buildPayment(PAYMENT_ID, ORDER_ID, PaymentStatus.COMPLETED);

            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existingPayment));

            paymentCommandService.onInventoryReserved(event);

            verify(paymentRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }
    }

    // ─── onShipmentFailed (compensate) ───────────────────────

    @Nested
    @DisplayName("onShipmentFailed() — compensation / refund")
    class OnShipmentFailed {

        @Test
        @DisplayName("SUCCESS — completed payment gets refunded and publishes payment.refunded")
        void onShipmentFailed_refundsPayment() {
            var event = ShipmentStartedEvent.failure("ship-001", ORDER_ID, "No carrier available");
            var payment = buildPayment(PAYMENT_ID, ORDER_ID, PaymentStatus.COMPLETED);

            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            paymentCommandService.onShipmentFailed(event);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            verify(paymentRepository).save(payment);

            ArgumentCaptor<PaymentRefundedEvent> captor = ArgumentCaptor.forClass(PaymentRefundedEvent.class);
            verify(kafkaTemplate).send(eq("payment.refunded"), eq(ORDER_ID), captor.capture());
            assertThat(captor.getValue().orderId()).isEqualTo(ORDER_ID);
            assertThat(captor.getValue().paymentId()).isEqualTo(PAYMENT_ID);
        }

        @Test
        @DisplayName("FAILURE — no completed payment found, no refund action")
        void onShipmentFailed_noCompletedPayment_noAction() {
            var event = ShipmentStartedEvent.failure("ship-001", ORDER_ID, "No carrier available");
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

            paymentCommandService.onShipmentFailed(event);

            verify(paymentRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("FAILURE — payment already FAILED (not COMPLETED), no refund")
        void onShipmentFailed_paymentAlreadyFailed_noRefund() {
            var event = ShipmentStartedEvent.failure("ship-001", ORDER_ID, "No carrier available");
            var payment = buildPayment(PAYMENT_ID, ORDER_ID, PaymentStatus.FAILED);

            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

            paymentCommandService.onShipmentFailed(event);

            verify(paymentRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }
    }

    // ─── Helper ──────────────────────────────────────────────

    private Payment buildPayment(String id, String orderId, PaymentStatus status) {
        var payment = new Payment();
        payment.setId(id);
        payment.setOrderId(orderId);
        payment.setStatus(status);
        return payment;
    }
}
