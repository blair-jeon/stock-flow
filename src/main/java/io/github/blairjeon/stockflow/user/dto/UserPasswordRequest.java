package io.github.blairjeon.stockflow.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserPasswordRequest(
        @NotBlank String password
) {
}