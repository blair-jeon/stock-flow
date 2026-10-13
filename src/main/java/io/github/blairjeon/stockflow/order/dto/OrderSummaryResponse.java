package io.github.blairjeon.stockflow.order.dto;

import io.github.blairjeon.stockflow.order.Order;
import io.github.blairjeon.stockflow.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long id,
        LocalDateTime orderedAt,
        String representativeProductName,
        int itemCount,
        OrderStatus orderStatus,
        BigDecimal totalAmount
) {
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getCreatedAt(),
                order.getRepresentativeProductName(),
                order.getLineItemCount(),
                order.getStatus(),
                order.getTotalAmount().amount()
        );
    }
}