package io.github.blairjeon.stockflow.security.jwt;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Invalid authentication token.");
    }
}
