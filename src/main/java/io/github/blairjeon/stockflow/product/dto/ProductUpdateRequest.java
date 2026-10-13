package io.github.blairjeon.stockflow.product.dto;

import io.github.blairjeon.stockflow.product.ProductStatus;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        String name,

        @PositiveOrZero
        BigDecimal price,

        @PositiveOrZero
        Integer stockQuantity,

        ProductStatus productStatus
) {
}