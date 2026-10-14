package io.github.blairjeon.stockflow.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Login ID is required.")
        String loginId,

        @NotBlank(message = "Password is required.")
        String password

) {
}