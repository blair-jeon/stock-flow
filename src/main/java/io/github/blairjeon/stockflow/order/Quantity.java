package io.github.blairjeon.stockflow.order;

import io.github.blairjeon.stockflow.common.UnitCount;
import jakarta.persistence.Embeddable;

@Embeddable
public record Quantity(int value) implements UnitCount {
    public Quantity{
        if(value <1){
            throw new IllegalArgumentException("Quantity must not be zero or negative");
        }
    }
}