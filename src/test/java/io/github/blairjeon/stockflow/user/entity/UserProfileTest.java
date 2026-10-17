package io.github.blairjeon.stockflow.user.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserProfileTest {
    @Test
    void shouldCreateProfileWithProvidedDetails(){
        UserProfile profile = UserProfile.create(
                1L,
                "test-last-name",
                "test-first-name",
                "000-000-0000",
                "test@email.com",
                "test-address"
        );

        assertEquals(1L, profile.getUserId());
        assertEquals("test-last-name", profile.getLastName());
        assertEquals("test-first-name", profile.getFirstName());
        assertEquals("000-000-0000", profile.getPhoneNumber());
        assertEquals("test@email.com", profile.getEmail());
        assertEquals("test-address", profile.getAddress());
    }

    @Test
    void shouldUpdateProfileWithProvidedDetails(){
        UserProfile profile = UserProfile.create(
                1L,
                "test-old-last-name",
                "test-old-first-name",
                "000-000-0000",
                "test-old@email.com",
                "test-old-address"
        );

        profile.updateProfile(
                "test-new-last-name",
                "test-new-first-name",
                "111-111-1111",
                "test-new@email.com",
                "test-new-address"
        );

        assertEquals(1L, profile.getUserId());
        assertEquals("test-new-last-name", profile.getLastName());
        assertEquals("test-new-first-name", profile.getFirstName());
        assertEquals("111-111-1111", profile.getPhoneNumber());
        assertEquals("test-new@email.com", profile.getEmail());
        assertEquals("test-new-address", profile.getAddress());

    }

}