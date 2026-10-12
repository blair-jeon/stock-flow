package io.github.blairjeon.stockflow.user.exception;

public class PasswordMismatchException extends RuntimeException {
    public PasswordMismatchException() {
        super("Current password is incorrect.");
    }
}
