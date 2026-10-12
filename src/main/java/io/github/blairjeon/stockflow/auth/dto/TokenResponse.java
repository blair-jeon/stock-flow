package io.github.blairjeon.stockflow.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}