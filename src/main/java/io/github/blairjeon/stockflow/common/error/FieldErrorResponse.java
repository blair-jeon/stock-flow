package io.github.blairjeon.stockflow.common.error;

public record FieldErrorResponse(
        String field,
        String message
) {
}