package com.phuchoccode.OrderService.domain.statemachine;

import com.phuchoccode.OrderService.domain.exception.IllegalStateTransitionException;
import com.phuchoccode.OrderService.domain.valueobject.OrderStatus;

import java.util.Map;
import java.util.Set;

public class OrderStatusTransition {

    // Map: trạng thái hiện tại → các trạng thái được phép chuyển sang
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(
                    OrderStatus.INVENTORY_RESERVING,
                    OrderStatus.CANCELLED
            ),
            OrderStatus.INVENTORY_RESERVING, Set.of(
                    OrderStatus.PAYMENT_PROCESSING,
                    OrderStatus.CANCELLED
            ),
            OrderStatus.PAYMENT_PROCESSING, Set.of(
                    OrderStatus.SHIPMENT_PROCESSING,
                    OrderStatus.CANCELLED
            ),
            OrderStatus.SHIPMENT_PROCESSING, Set.of(
                    OrderStatus.COMPLETED,
                    OrderStatus.CANCELLED
            ),
            OrderStatus.COMPLETED,  Set.of(),  // terminal — không đi đâu được
            OrderStatus.CANCELLED,  Set.of()   // terminal — không đi đâu được
    );

    public static boolean isValid(OrderStatus from, OrderStatus to) {
        return VALID_TRANSITIONS
                .getOrDefault(from, Set.of())
                .contains(to);
    }

    public static void validate(OrderStatus from, OrderStatus to) {
        if (!isValid(from, to)) {
            throw new IllegalStateTransitionException(
                    String.format("Invalid transition: %s → %s", from, to)
            );
        }
    }
}