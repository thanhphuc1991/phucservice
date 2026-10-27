package com.phuchoccode.OrderService.application.service;

import com.phuchoccode.OrderService.domain.port.inbound.CreateOrderUseCase;
import com.phuchoccode.OrderService.infrastructure.persistence.entity.OrderCreationRequest;
import com.phuchoccode.OrderService.infrastructure.persistence.repository.OrderCreationRequestRepository;
import com.phuchoccode.OrderService.infrastructure.web.OrderResponse;
import com.phuchoccode.OrderService.request.CreateOrderRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
public class IdempotentOrderCreationService {

    private final OrderCreationRequestRepository repository;
    private final CreateOrderUseCase createOrderUseCase;
    private final TransactionTemplate transaction;

    public IdempotentOrderCreationService(
            OrderCreationRequestRepository repository,
            CreateOrderUseCase createOrderUseCase,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.createOrderUseCase = createOrderUseCase;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public OrderResponse create(String key, CreateOrderRequest request) {
        if (key == null || key.isBlank() || key.length() > 128) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must contain 1–128 characters"
            );
        }

        try {
            return transaction.execute(tx -> {
                var existing = repository.findById(key);

                if (existing.isPresent()) {
                    return replay(existing.get(), request);
                }

                var entry = new OrderCreationRequest();
                entry.setIdempotencyKey(key);
                entry.setCustomerId(request.customerId());
                entry.setProductId(request.productId());
                entry.setQuantity(request.quantity());
                entry.setRequestedAmount(request.totalAmount());

                // Insert và flush trước khi tạo order.
                // Unique key trong DB quyết định request nào được đi tiếp.
                repository.saveAndFlush(entry);

                var order = createOrderUseCase.createOrder(request);

                entry.setOrderId(order.orderId().value());
                entry.setResponseStatus(order.status().name());
                entry.setResponseAmount(
                        order.totalAmount().amount().doubleValue()
                );

                repository.saveAndFlush(entry);

                return response(entry);
            });
        } catch (DataIntegrityViolationException ex) {
            // Transaction phía trên đã rollback trước khi đến đây.
            // Nếu request khác đã commit cùng key, trả response của nó.
            return transaction.execute(tx -> {
                var existing = repository.findById(key);

                if (existing.isEmpty()) {
                    // Không có record cùng key: đây là lỗi dữ liệu khác.
                    throw ex;
                }

                return replay(existing.get(), request);
            });
        }
    }

    private OrderResponse replay(
            OrderCreationRequest entry,
            CreateOrderRequest request) {

        boolean sameRequest =
                Objects.equals(entry.getCustomerId(), request.customerId())
                        && Objects.equals(entry.getProductId(), request.productId())
                        && entry.getQuantity() == request.quantity()
                        && Double.compare(
                        entry.getRequestedAmount(),
                        request.totalAmount()
                ) == 0;

        if (!sameRequest) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency-Key was already used with a different request"
            );
        }

        return response(entry);
    }

    private OrderResponse response(OrderCreationRequest entry) {
        return new OrderResponse(
                entry.getOrderId(),
                entry.getCustomerId(),
                entry.getResponseStatus(),
                entry.getResponseAmount()
        );
    }
}