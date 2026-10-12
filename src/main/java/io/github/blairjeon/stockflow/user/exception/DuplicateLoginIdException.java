package io.github.blairjeon.stockflow.user.exception;

public class DuplicateLoginIdException extends RuntimeException {
    public DuplicateLoginIdException() {
        super("Login ID is already in use.");
    }
}
