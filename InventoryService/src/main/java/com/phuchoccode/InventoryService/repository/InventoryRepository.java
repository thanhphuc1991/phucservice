package com.phuchoccode.InventoryService.repository;

import com.phuchoccode.InventoryService.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, String> {
    Optional<Inventory> findByReservedOrderId(String orderId);

}
