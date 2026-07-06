package com.phuchoccode.InventoryService.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Inventory {
    @Id
    private String productId;
    private int availableQuantity;
    private int reservedQuantity;
}
