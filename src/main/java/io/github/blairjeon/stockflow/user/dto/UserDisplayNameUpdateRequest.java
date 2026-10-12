package io.github.blairjeon.stockflow.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserDisplayNameUpdateRequest(
        @NotBlank
        String password,

        @NotBlank
        String displayName
) {
}