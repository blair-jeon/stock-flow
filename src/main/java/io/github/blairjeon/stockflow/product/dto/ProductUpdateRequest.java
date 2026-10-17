package io.github.blairjeon.stockflow.product.dto;

import io.github.blairjeon.stockflow.product.entity.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        String name,

        @DecimalMin("0.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal unitPrice,

        ProductStatus status
) {
}