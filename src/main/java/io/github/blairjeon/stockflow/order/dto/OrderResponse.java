package io.github.blairjeon.stockflow.order.dto;

import io.github.blairjeon.stockflow.order.entity.Order;
import io.github.blairjeon.stockflow.order.entity.OrderStatus;

import java.util.List;

public record OrderResponse(
        Long orderId,
        OrderStatus status,
        String name,
        int lineCount,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order, List<OrderItemResponse> items){
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getName(),
                order.getLineCount(),
                items
        );
    }
}