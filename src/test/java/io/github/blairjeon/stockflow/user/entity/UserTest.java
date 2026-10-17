package io.github.blairjeon.stockflow.user.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {
    @Test
    void shouldCreateUserWhenDefaultRoleAndStatus(){
        User user = User.create("test-loginId", "test-encoded-password");

        assertAll(
                () -> assertEquals("test-loginId", user.getLoginId()),
                () -> assertEquals("test-encoded-password", user.getPasswordHash()),
                () -> assertEquals(UserRole.USER, user.getRole()),
                () -> assertEquals(UserStatus.ACTIVE, user.getStatus())
        );
    }

    @Test
    void shouldUpdatePasswordHash(){
        User user = User.create("test-loginId", "test-old-hash");

        user.updatePassword("test-new-hash");

        assertEquals("test-new-hash", user.getPasswordHash());
    }

    @Test
    void shouldRejectNullPasswordHashWhenUpdatingPassword(){
        User user = User.create("test-loginId", "test-old-hash");

        assertThrows(
                NullPointerException.class,
                () -> user.updatePassword(null)
        );

        assertEquals("test-old-hash", user.getPasswordHash());
    }
}