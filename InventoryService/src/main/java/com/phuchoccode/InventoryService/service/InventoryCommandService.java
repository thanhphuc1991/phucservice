package com.phuchoccode.InventoryService.service;

import com.phuchoccode.CommonnService.common.event.*;
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
        inventoryRepository.save(inv);

        // Dùng static factory method success() từ record mới
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
        // Dùng static factory method failure() từ record mới
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

    // ─── Compensate — tách thành 2 method riêng ──────────────

    @KafkaListener(topics = "payment.failed", groupId = "inventory-service-group")
    public void onPaymentFailed(PaymentProcessedEvent event) {
        releaseStock(event.orderId());
    }

    @KafkaListener(topics = "shipment.failed", groupId = "inventory-service-group")
    public void onShipmentFailed(ShipmentStartedEvent event) {
        releaseStock(event.orderId());
    }

    private void releaseStock(String orderId) {
        log.info("Compensating inventory — orderId={}", orderId);

        // Paste Inventory entity lên để mình fix phần này chính xác
        inventoryRepository.findByReservedOrderId(orderId)
                .ifPresent(inv -> {
                    inv.setAvailableQuantity(inv.getAvailableQuantity() + inv.getReservedQuantity());
                    inv.setReservedQuantity(0);
                    inventoryRepository.save(inv);

                    kafkaTemplate.send("inventory.released", orderId,
                            new InventoryReleasedEvent(
                                    orderId,
                                    inv.getProductId(),
                                    inv.getReservedQuantity()
                            )
                    );
                    log.info("Inventory released — orderId={}", orderId);
                });
    }
}