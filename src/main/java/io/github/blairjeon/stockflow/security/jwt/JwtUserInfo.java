package io.github.blairjeon.stockflow.security.jwt;

import io.github.blairjeon.stockflow.user.UserRole;

public record JwtUserInfo(
        Long userId,
        UserRole userRole
) {
}