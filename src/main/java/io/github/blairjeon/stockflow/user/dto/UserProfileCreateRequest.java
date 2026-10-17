package io.github.blairjeon.stockflow.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserProfileCreateRequest(
        @NotBlank String lastName,
        @NotBlank String firstName,
        @NotBlank String phoneNumber,
        @NotBlank String email,
        @NotBlank String address
) {
}