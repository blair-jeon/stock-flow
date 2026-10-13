package io.github.blairjeon.stockflow.product.dto;

import io.github.blairjeon.stockflow.product.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        int stockQuantity
) {
    public static ProductResponse from(Product product){
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice().amount(),
                product.getStockQuantity().value()
        );
    }
}