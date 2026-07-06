// repository/PaymentRepository.java
package com.phuchoccode.PaymentService.repository;

import com.phuchoccode.PaymentService.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByOrderId(String orderId);
}