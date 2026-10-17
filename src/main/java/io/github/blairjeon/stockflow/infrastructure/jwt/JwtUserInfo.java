package io.github.blairjeon.stockflow.infrastructure.jwt;

import io.github.blairjeon.stockflow.user.entity.User;
import io.github.blairjeon.stockflow.user.entity.UserRole;

public record JwtUserInfo(
        Long userId,
        UserRole role
) {
    public static JwtUserInfo from(User user){
        return new JwtUserInfo(
                user.getId(),
                user.getRole()
        );
    }
}