package io.github.blairjeon.stockflow.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank(message = "Product name is required.")
        String name,

        @NotNull(message = "Price is required.")
        @PositiveOrZero(message = "Price must be zero or greater.")
        BigDecimal price,

        @NotNull(message = "Stock quantity is required.")
        @PositiveOrZero(message = "Stock quantity must be zero or greater.")
        Integer stockQuantity
) {
}