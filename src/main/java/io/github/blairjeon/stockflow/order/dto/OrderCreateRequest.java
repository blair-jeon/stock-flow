package io.github.blairjeon.stockflow.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderCreateRequest(

        @NotEmpty(message = "Please add at least one order item.")
        List<
                @NotNull(message = "Order item must not be null.")
                @Valid OrderItemRequest
                > items

) {
}