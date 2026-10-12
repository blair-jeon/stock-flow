package io.github.blairjeon.stockflow.user.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("User not found.");
    }
}
