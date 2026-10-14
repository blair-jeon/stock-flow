package io.github.blairjeon.stockflow.order.repository;

import io.github.blairjeon.stockflow.order.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUserIdAndId(Long userId, Long id);
    Slice<Order> findAllByUserId(Long userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithLockByUserIdAndId(Long userId, Long id);
}