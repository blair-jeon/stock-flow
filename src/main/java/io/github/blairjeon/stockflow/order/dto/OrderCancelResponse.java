package io.github.blairjeon.stockflow.order.dto;

import io.github.blairjeon.stockflow.order.entity.Order;
import io.github.blairjeon.stockflow.order.entity.OrderStatus;

import java.math.BigDecimal;

public record OrderCancelResponse(
        Long orderId,
        BigDecimal amount,
        OrderStatus status
) {
    public static OrderCancelResponse from(Order order){
        return new OrderCancelResponse(order.getId(), order.getAmount(), order.getStatus());
    }
}