package io.github.blairjeon.stockflow.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank
        String name,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal unitPrice,

        @NotNull
        @PositiveOrZero
        Integer stock
) {
}