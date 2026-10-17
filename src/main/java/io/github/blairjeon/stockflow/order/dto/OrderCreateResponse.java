package io.github.blairjeon.stockflow.order.dto;


import io.github.blairjeon.stockflow.order.entity.Order;
import io.github.blairjeon.stockflow.order.entity.OrderStatus;

public record OrderCreateResponse(
        Long orderId,
        int lineCount,
        OrderStatus status
) {
    public static OrderCreateResponse from(Order order){
        return new OrderCreateResponse(order.getId(), order.getLineCount(), order.getStatus());
    }
}