package io.github.blairjeon.stockflow.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderCreateRequest(
        @NotEmpty
        List<@NotNull @Valid OrderItemRequest> items
) {
}