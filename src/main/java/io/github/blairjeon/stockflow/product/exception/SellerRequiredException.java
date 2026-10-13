package io.github.blairjeon.stockflow.product.exception;

public class SellerRequiredException extends RuntimeException {
    public SellerRequiredException() {
        super("Seller role is required.");
    }
}
