package com.phuchoccode.ShipmentService.repository;

import com.phuchoccode.ShipmentService.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, String> {

    Optional<Shipment> findByOrderId(String orderId);
}