package io.github.blairjeon.stockflow.order.dto;

import io.github.blairjeon.stockflow.order.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice().amount(),
                item.getQuantity().value()
        );
    }
}