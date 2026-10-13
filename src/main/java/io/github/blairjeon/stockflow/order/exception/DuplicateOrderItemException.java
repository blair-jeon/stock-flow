package io.github.blairjeon.stockflow.order.exception;

public class DuplicateOrderItemException extends RuntimeException {
    public DuplicateOrderItemException() {
        super("Duplicate products are not allowed in an order.");
    }
}
