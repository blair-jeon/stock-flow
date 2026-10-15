package io.github.blairjeon.stockflow.user.dto;

import io.github.blairjeon.stockflow.user.User;
import io.github.blairjeon.stockflow.user.UserRole;

public record UserResponse(
        String loginId,
        String displayName,
        UserRole userRole
) {
    public static UserResponse from(User user){
        return new UserResponse(user.getLoginId(), user.getDisplayName(), user.getRole());
    }
}