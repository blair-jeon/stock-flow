package io.github.blairjeon.stockflow.common.error;

public record ErrorResponse(
        int status,
        String message
) {
}