package io.github.blairjeon.stockflow.auth.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid login ID or password.");
    }
}
