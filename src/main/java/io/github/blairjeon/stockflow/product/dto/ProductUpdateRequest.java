package io.github.blairjeon.stockflow.product.dto;

import io.github.blairjeon.stockflow.product.ProductStatus;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        String name,

        @PositiveOrZero(message = "Price must be zero or greater.")
        BigDecimal price,

        @PositiveOrZero(message = "Stock quantity must be zero or greater.")
        Integer stockQuantity,

        ProductStatus productStatus
) {
}