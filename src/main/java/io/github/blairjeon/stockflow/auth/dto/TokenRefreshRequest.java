package io.github.blairjeon.stockflow.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
        @NotBlank String token
) {
}