package com.phuchoccode.InventoryService.repository;

import com.phuchoccode.InventoryService.entity.InventoryReservation;
import com.phuchoccode.InventoryService.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// repository/InventoryReservationRepository.java
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {
    Optional<InventoryReservation> findByOrderIdAndStatus(String orderId, ReservationStatus status);
}
