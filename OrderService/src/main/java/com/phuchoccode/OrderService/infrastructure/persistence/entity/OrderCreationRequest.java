package com.phuchoccode.OrderService.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_creation_requests")
@Getter
@Setter
@NoArgsConstructor
public class OrderCreationRequest {

    // Primary key ngăn hai request đồng thời dùng cùng key.
    @Id
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String productId;

    private int quantity;
    private double requestedAmount;

    // Response của lần tạo đầu tiên.
    private String orderId;
    private String responseStatus;
    private double responseAmount;
}
