package io.github.blairjeon.stockflow.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserCreateRequest(
        @NotBlank
        String login,

        @NotBlank
        String password,

        @NotBlank
        String displayName
) {
}