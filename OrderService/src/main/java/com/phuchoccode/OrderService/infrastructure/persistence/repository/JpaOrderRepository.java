package com.phuchoccode.OrderService.infrastructure.persistence.repository;

import com.phuchoccode.OrderService.domain.aggregate.OrderAggregate;
import com.phuchoccode.OrderService.domain.repository.OrderRepository;
import com.phuchoccode.OrderService.domain.valueobject.OrderId;
import com.phuchoccode.OrderService.infrastructure.mapper.OrderMapper;
import com.phuchoccode.OrderService.infrastructure.persistence.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository repository;
    private final OrderMapper mapper;

    @Override
    public OrderAggregate save(OrderAggregate aggregate) {

        Order entity = mapper.toJpaEntity(aggregate);

        repository.save(entity);

        return aggregate;
    }

    @Override
    public Optional<OrderAggregate> findById(OrderId id) {

        return repository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public boolean exists(OrderId id) {

        return repository.existsById(id.value());
    }

    @Override
    public void delete(OrderId id) {

        repository.deleteById(id.value());
    }
}
