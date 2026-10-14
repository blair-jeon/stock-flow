package io.github.blairjeon.stockflow.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserCreateRequest(

        @NotBlank(message = "Login ID is required.")
        String login,

        @NotBlank(message = "Password is required.")
        String password,

        @NotBlank(message = "Display name is required.")
        String displayName

) {
}