package io.github.blairjeon.stockflow.order.repository;

import io.github.blairjeon.stockflow.order.entity.Order;
import io.github.blairjeon.stockflow.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findAllByOrder(Order order);
}