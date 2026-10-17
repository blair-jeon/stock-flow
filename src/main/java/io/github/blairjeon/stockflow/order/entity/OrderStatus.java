package io.github.blairjeon.stockflow.order.entity;

public enum OrderStatus {
    CREATED,
    CONFIRMED,
    SHIPPED,
    COMPLETED,
    CANCELLED;

    public boolean canCancel(){
        return this == CREATED;
    }
}