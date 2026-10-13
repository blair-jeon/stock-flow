package io.github.blairjeon.stockflow.order.dto;

import io.github.blairjeon.stockflow.order.Order;
import io.github.blairjeon.stockflow.order.OrderItem;
import io.github.blairjeon.stockflow.order.OrderStatus;

import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus orderStatus,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order, List<OrderItem> items) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                items.stream()
                        .map(OrderItemResponse::from)
                        .toList()
        );
    }
}