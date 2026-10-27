package com.phuchoccode.InventoryService.entity;

import com.phuchoccode.InventoryService.model.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// entity/InventoryReservation.java
@Entity
@Table(name = "inventory_reservation")
@Getter
@Setter
@NoArgsConstructor
public class InventoryReservation {

    @Id
    private String id;
    private String orderId;
    private String productId;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;  // RESERVED, RELEASED
}