package io.github.blairjeon.stockflow.product.dto;

import io.github.blairjeon.stockflow.product.entity.Product;
import io.github.blairjeon.stockflow.product.entity.ProductStatus;
import io.github.blairjeon.stockflow.user.entity.User;

import java.math.BigDecimal;

public record ProductSummaryResponse(
        Long productId,
        Long userId,
        String loginId,
        String productName,
        BigDecimal unitPrice,
        ProductStatus productStatus
) {
    public static ProductSummaryResponse from(User user, Product product){
        return new ProductSummaryResponse(
                product.getId(),
                user.getId(),
                user.getLoginId(),
                product.getName(),
                product.getUnitPrice(),
                product.getStatus()
        );
    }
}