package io.github.blairjeon.stockflow.order;

public enum OrderStatus {
    PENDING,
    PAID,
    DELIVERING,
    DELIVERED,
    COMPLETED,
    CANCELED;

    public boolean allowsTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == PAID || target == CANCELED;
            case PAID -> target == DELIVERING || target == CANCELED;
            case DELIVERING -> target == DELIVERED;
            case DELIVERED -> target == COMPLETED || target == CANCELED;
            case COMPLETED, CANCELED -> false;
        };
    }
}