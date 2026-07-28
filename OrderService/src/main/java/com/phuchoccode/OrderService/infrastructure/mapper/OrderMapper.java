// infrastructure/persistence/mapper/OrderMapper.java
package com.phuchoccode.OrderService.infrastructure.mapper;

import com.phuchoccode.OrderService.domain.aggregate.OrderAggregate;
import com.phuchoccode.OrderService.domain.entity.OrderItem;
import com.phuchoccode.OrderService.domain.valueobject.CustomerId;
import com.phuchoccode.OrderService.domain.valueobject.Money;
import com.phuchoccode.OrderService.domain.valueobject.OrderId;
import com.phuchoccode.OrderService.domain.valueobject.ProductId;
import com.phuchoccode.OrderService.infrastructure.persistence.entity.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    // Aggregate → JpaEntity (để lưu vào DB)
    public Order toJpaEntity(OrderAggregate aggregate) {
        var entity = new Order();
        entity.setId(aggregate.orderId().value());
        entity.setCustomerId(aggregate.customerId().value());
        entity.setStatus(aggregate.status());
        entity.setTotalAmount(aggregate.totalAmount().amount().doubleValue());

        // Lấy item đầu tiên — đơn giản hoá cho demo
        aggregate.items().stream().findFirst().ifPresent(item -> {
            entity.setProductId(item.productId().value());
            entity.setQuantity(item.quantity());
        });

        return entity;
    }

    // JpaEntity → Aggregate (để load từ DB)
    public OrderAggregate toDomain(Order entity) {
        var customerId = CustomerId.of(entity.getCustomerId());
        var productId  = ProductId.of(entity.getProductId());
        var unitPrice  = Money.of(entity.getTotalAmount() / entity.getQuantity());
        var item       = new OrderItem(productId, entity.getQuantity(), unitPrice);

        return OrderAggregate.restore(
                OrderId.of(entity.getId()),
                customerId,
                List.of(item),
                entity.getStatus()
        );
    }
}