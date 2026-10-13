package io.github.blairjeon.stockflow.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserTest {
    @Test
    void shouldAssignUserRoleWhenCreated(){
        User user = User.create(
                "alice",
                "test-password-hash",
                "Alice"
        );

        assertEquals(UserRole.USER, user.getUserRole());
    }
}