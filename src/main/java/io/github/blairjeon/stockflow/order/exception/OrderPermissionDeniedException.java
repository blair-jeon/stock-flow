package io.github.blairjeon.stockflow.order.exception;

public class OrderPermissionDeniedException extends RuntimeException {
    public OrderPermissionDeniedException() {
        super("Only users with the USER role can place orders.");
    }
}
