package io.github.blairjeon.stockflow.order.dto;

import io.github.blairjeon.stockflow.order.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice
) {
    public static OrderItemResponse from(OrderItem orderItem){
        return new OrderItemResponse(
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice()
        );
    }
}