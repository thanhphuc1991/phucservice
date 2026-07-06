package com.phuchoccode.PaymentService.service;

import com.phuchoccode.CommonnService.common.event.InventoryReservedEvent;
import com.phuchoccode.CommonnService.common.event.PaymentProcessedEvent;
import com.phuchoccode.CommonnService.common.event.PaymentRefundedEvent;
import com.phuchoccode.CommonnService.common.event.ShipmentStartedEvent;
import com.phuchoccode.PaymentService.entity.Payment;
import com.phuchoccode.PaymentService.model.PaymentStatus;
import com.phuchoccode.PaymentService.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCommandService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ─── Step 2: Lắng nghe InventoryReservedEvent ────────────

    @KafkaListener(topics = "inventory.reserved", groupId = "payment-service-group")
    public void onInventoryReserved(InventoryReservedEvent event) {

        // Chỉ xử lý khi inventory reserved thành công
        if (!event.reserved()) {
            log.info("Inventory not reserved, skipping payment — orderId={}", event.orderId());
            return;
        }

        log.info("Processing payment — orderId={}", event.orderId());

        // Idempotency check
        boolean alreadyProcessed = paymentRepository
                .findByOrderId(event.orderId())
                .isPresent();

        if (alreadyProcessed) {
            log.warn("Payment already processed — orderId={}", event.orderId());
            return;
        }

        String paymentId = UUID.randomUUID().toString();
        boolean success  = processPayment(event.orderId());

        if (success) {
            var payment = new Payment();
            payment.setId(paymentId);
            payment.setOrderId(event.orderId());
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);

            kafkaTemplate.send("payment.processed", event.orderId(),
                    PaymentProcessedEvent.success(paymentId, event.orderId())
            );
            log.info("Payment COMPLETED — orderId={}", event.orderId());

        } else {
            var payment = new Payment();
            payment.setId(paymentId);
            payment.setOrderId(event.orderId());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Insufficient balance");
            paymentRepository.save(payment);

            kafkaTemplate.send("payment.failed", event.orderId(),
                    PaymentProcessedEvent.failure(paymentId, event.orderId(), "Insufficient balance")
            );
            log.warn("Payment FAILED — orderId={}", event.orderId());
        }
    }

    // ─── Compensate: hoàn tiền khi shipment fail ─────────────

    @KafkaListener(topics = "shipment.failed", groupId = "payment-service-group")
    public void onShipmentFailed(ShipmentStartedEvent event) {
        log.info("Refunding payment — orderId={}", event.orderId());

        paymentRepository.findByOrderId(event.orderId())
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .ifPresentOrElse(
                        payment -> {
                            payment.setStatus(PaymentStatus.REFUNDED);
                            paymentRepository.save(payment);

                            kafkaTemplate.send("payment.refunded", event.orderId(),
                                    new PaymentRefundedEvent(
                                            event.orderId(),
                                            payment.getId()
                                    )
                            );
                            log.info("Payment REFUNDED — orderId={}", event.orderId());
                        },
                        () -> log.warn("No completed payment to refund — orderId={}", event.orderId())
                );
    }

    // ─── Mock ─────────────────────────────────────────────────

    private boolean processPayment(String orderId) {
        return true;
    }
}