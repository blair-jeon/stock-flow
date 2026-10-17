package io.github.blairjeon.stockflow.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserPasswordUpdateRequest(
        @NotBlank String oldPassword,
        @NotBlank String newPassword
) {
}