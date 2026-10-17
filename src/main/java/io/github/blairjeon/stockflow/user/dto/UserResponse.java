package io.github.blairjeon.stockflow.user.dto;

import io.github.blairjeon.stockflow.user.entity.User;
import io.github.blairjeon.stockflow.user.entity.UserRole;
import io.github.blairjeon.stockflow.user.entity.UserStatus;

public record UserResponse(
        String loginId,
        UserRole role,
        UserStatus status
) {
    public static UserResponse from(User user){
        return new UserResponse(
                user.getLoginId(),
                user.getRole(),
                user.getStatus()
        );
    }
}