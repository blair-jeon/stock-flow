package io.github.blairjeon.stockflow.product.dto;

import jakarta.validation.constraints.NotNull;

public record InventoryAdjustmentRequest(
        @NotNull
        Integer adjustment
) {
}