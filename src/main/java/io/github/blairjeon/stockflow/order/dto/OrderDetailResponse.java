package io.github.blairjeon.stockflow.order.dto;

import io.github.blairjeon.stockflow.order.entity.Order;
import io.github.blairjeon.stockflow.order.entity.OrderStatus;

import java.math.BigDecimal;

public record OrderDetailResponse(
        Long id,
        OrderStatus status,
        BigDecimal amount,
        String name,
        int lineCount
) {
    public static OrderDetailResponse from(Order order){
        return new OrderDetailResponse(
                order.getId(),
                order.getStatus(),
                order.getAmount(),
                order.getName(),
                order.getLineCount()
        );
    }
}