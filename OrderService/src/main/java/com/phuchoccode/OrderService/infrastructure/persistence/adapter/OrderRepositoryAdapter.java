// infrastructure/persistence/adapter/OrderRepositoryAdapter.java
package com.phuchoccode.OrderService.infrastructure.persistence.adapter;

import com.phuchoccode.OrderService.domain.aggregate.OrderAggregate;
import com.phuchoccode.OrderService.domain.port.outbound.OrderRepositoryPort;
import com.phuchoccode.OrderService.domain.valueobject.OrderId;
import com.phuchoccode.OrderService.infrastructure.persistence.mapper.OrderMapper;
import com.phuchoccode.OrderService.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public OrderAggregate save(OrderAggregate aggregate) {
        var entity = mapper.toJpaEntity(aggregate);
        jpaRepository.save(entity);
        return aggregate;
    }

    @Override
    public Optional<OrderAggregate> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.value())
                .map(mapper::toDomain);
    }
}