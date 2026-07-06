package com.phuchoccode.ShipmentService.service;

import com.phuchoccode.CommonnService.common.event.PaymentProcessedEvent;
import com.phuchoccode.CommonnService.common.event.ShipmentStartedEvent;
import com.phuchoccode.ShipmentService.entity.Shipment;
import com.phuchoccode.ShipmentService.model.ShipmentStatus;
import com.phuchoccode.ShipmentService.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentCommandService {

    private final ShipmentRepository shipmentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ─── Step 3: Lắng nghe PaymentProcessedEvent ─────────────

    @KafkaListener(topics = "payment.processed", groupId = "shipment-service-group")
    public void onPaymentProcessed(PaymentProcessedEvent event) {

        // Chỉ xử lý khi payment thành công
        if (!event.success()) {
            log.info("Payment not success, skipping shipment — orderId={}", event.orderId());
            return;
        }

        log.info("Starting shipment — orderId={}", event.orderId());

        // Idempotency check
        boolean alreadyProcessed = shipmentRepository
                .findByOrderId(event.orderId())
                .isPresent();

        if (alreadyProcessed) {
            log.warn("Shipment already processed — orderId={}", event.orderId());
            return;
        }

        String shipmentId = UUID.randomUUID().toString();
        boolean success   = arrangeShipment(event.orderId());

        if (success) {
            var shipment = new Shipment();
            shipment.setId(shipmentId);
            shipment.setOrderId(event.orderId());
            shipment.setStatus(ShipmentStatus.STARTED);
            shipmentRepository.save(shipment);

            kafkaTemplate.send("shipment.started", event.orderId(),
                    ShipmentStartedEvent.success(shipmentId, event.orderId())
            );
            log.info("Shipment STARTED — orderId={}", event.orderId());

        } else {
            var shipment = new Shipment();
            shipment.setId(shipmentId);
            shipment.setOrderId(event.orderId());
            shipment.setStatus(ShipmentStatus.FAILED);
            shipment.setFailureReason("No carrier available");
            shipmentRepository.save(shipment);

            kafkaTemplate.send("shipment.failed", event.orderId(),
                    ShipmentStartedEvent.failure(shipmentId, event.orderId(), "No carrier available")
            );
            log.warn("Shipment FAILED — orderId={}", event.orderId());
        }
    }

    // ─── Mock ─────────────────────────────────────────────────

    private boolean arrangeShipment(String orderId) {
        return true;
    }
}