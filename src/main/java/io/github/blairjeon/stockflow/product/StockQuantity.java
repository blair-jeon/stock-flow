package io.github.blairjeon.stockflow.product;

import io.github.blairjeon.stockflow.order.Quantity;
import io.github.blairjeon.stockflow.common.UnitCount;
import jakarta.persistence.Embeddable;

@Embeddable
public record StockQuantity(int value) implements UnitCount {
    public StockQuantity{
        if(value <0){
            throw new IllegalArgumentException("Stock value must not be negative");
        }
    }

    public StockQuantity add(Quantity quantity){
        return new StockQuantity(value + quantity.value());
    }

    public StockQuantity subtract(UnitCount quantity){
        return new StockQuantity(value - quantity.value());
    }
}