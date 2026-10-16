package io.github.blairjeon.stockflow.order.dto;

import io.github.blairjeon.stockflow.order.Order;

public record OrderCreateResponse(
        Long orderId
) {
    public static OrderCreateResponse from(Order order){
        return new OrderCreateResponse(order.getId());
    }
}