package io.github.blairjeon.stockflow.user.dto;

import io.github.blairjeon.stockflow.user.entity.UserProfile;

public record UserProfileResponse(
        String lastName,
        String firstName,
        String phoneNumber,
        String email,
        String address
) {
    public static UserProfileResponse from(UserProfile profile){
        return new UserProfileResponse(
                profile.getLastName(),
                profile.getFirstName(),
                profile.getPhoneNumber(),
                profile.getEmail(),
                profile.getAddress()
        );
    }
}