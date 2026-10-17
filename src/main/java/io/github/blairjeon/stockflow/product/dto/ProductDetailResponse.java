package io.github.blairjeon.stockflow.product.dto;

import io.github.blairjeon.stockflow.product.entity.Inventory;
import io.github.blairjeon.stockflow.product.entity.Product;
import io.github.blairjeon.stockflow.product.entity.ProductStatus;
import io.github.blairjeon.stockflow.user.entity.User;

import java.math.BigDecimal;

public record ProductDetailResponse(
        Long productId,
        Long userId,
        String loginId,
        String productName,
        BigDecimal unitPrice,
        ProductStatus productStatus,
        int stockQuantity
) {
    public static ProductDetailResponse from(User user, Product product, Inventory inventory){
        return new ProductDetailResponse(
                product.getId(),
                product.getUserId(),
                user.getLoginId(),
                product.getName(),
                product.getUnitPrice(),
                product.getStatus(),
                inventory.getStockQuantity()
        );
    }
}