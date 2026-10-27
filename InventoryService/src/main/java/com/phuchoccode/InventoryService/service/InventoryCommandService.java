package com.phuchoccode.InventoryService.service;

import com.phuchoccode.CommonService.common.event.*;
import com.phuchoccode.InventoryService.entity.Inventory;
import com.phuchoccode.InventoryService.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryCommandService {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ─── Step 1: Lắng nghe OrderCreatedEvent ─────────────────

    @KafkaListener(topics = "order.created", groupId = "inventory-service-group")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Reserving inventory — orderId={}, productId={}",
                event.orderId(), event.productId());

        inventoryRepository.findById(event.productId())
                .filter(inv -> inv.getAvailableQuantity() >= event.quantity())
                .ifPresentOrElse(
                        inv -> reserveStock(inv, event),
                        ()  -> publishInventoryFailed(event, "Insufficient stock")
                );
    }

    private void reserveStock(Inventory inv, OrderCreatedEvent event) {
        inv.setAvailableQuantity(inv.getAvailableQuantity() - event.quantity());
        inv.setReservedQuantity(inv.getReservedQuantity() + event.quantity());
        inv.setReservedOrderId(event.orderId());    // ← lưu orderId để compensate sau
        inventoryRepository.save(inv);

        kafkaTemplate.send("inventory.reserved", event.orderId(),
                InventoryReservedEvent.success(
                        event.orderId(),
                        event.productId(),
                        event.quantity()
                )
        );
        log.info("Inventory reserved — orderId={}", event.orderId());
    }

    private void publishInventoryFailed(OrderCreatedEvent event, String reason) {
        kafkaTemplate.send("inventory.failed", event.orderId(),
                InventoryReservedEvent.failure(
                        event.orderId(),
                        event.productId(),
                        event.quantity(),
                        reason
                )
        );
        log.warn("Inventory FAILED — orderId={}, reason={}", event.orderId(), reason);
    }

    // ─── Compensate ───────────────────────────────────────────

    @KafkaListener(topics = "payment.failed", groupId = "inventory-service-group")
    public void onPaymentFailed(PaymentProcessedEvent event) {
        if (!event.success()) {
            releaseStock(event.orderId());
        }
    }

    @KafkaListener(topics = "shipment.failed", groupId = "inventory-service-group")
    public void onShipmentFailed(ShipmentStartedEvent event) {
        if (!event.success()) {
            releaseStock(event.orderId());
        }
    }

    private void releaseStock(String orderId) {
        log.info("Compensating inventory — orderId={}", orderId);

        inventoryRepository.findByReservedOrderId(orderId)
                .ifPresent(inv -> {
                    int quantityToRelease = inv.getReservedQuantity();

                    inv.setAvailableQuantity(inv.getAvailableQuantity() + quantityToRelease);
                    inv.setReservedQuantity(0);
                    inv.setReservedOrderId(null);    // ← clear orderId sau khi release
                    inventoryRepository.save(inv);

                    kafkaTemplate.send("inventory.released", orderId,
                            new InventoryReleasedEvent(
                                    orderId,
                                    inv.getProductId(),
                                    quantityToRelease    // ← dùng biến đã lưu trước khi set về 0
                            )
                    );
                    log.info("Inventory released — orderId={}, quantity={}",
                            orderId, quantityToRelease);
                });
    }
}