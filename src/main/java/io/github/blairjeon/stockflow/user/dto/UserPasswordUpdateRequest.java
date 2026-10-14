package io.github.blairjeon.stockflow.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserPasswordUpdateRequest(

        @NotBlank(message = "Current password is required.")
        String oldPassword,

        @NotBlank(message = "New password is required.")
        String newPassword

) {
}