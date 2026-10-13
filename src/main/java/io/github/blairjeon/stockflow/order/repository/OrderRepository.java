package io.github.blairjeon.stockflow.order.repository;

import io.github.blairjeon.stockflow.order.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUserIdAndId(Long userId, Long id);
    Slice<Order> findAllByUserId(Long userId, Pageable pageable);
}