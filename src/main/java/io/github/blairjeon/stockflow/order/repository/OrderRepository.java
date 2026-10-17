package io.github.blairjeon.stockflow.order.repository;

import io.github.blairjeon.stockflow.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithLockByUserIdAndId(Long userId, Long id);

    Optional<Order> findByUserIdAndId(Long userId, Long id);

    List<Order> findAllByUserId(Long userId, Pageable pageable);
}