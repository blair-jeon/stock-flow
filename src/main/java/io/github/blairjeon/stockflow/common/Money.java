package io.github.blairjeon.stockflow.common;

import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
public record Money(BigDecimal amount) {
    public Money{
        Objects.requireNonNull(amount, "Amount must not be null");

        if(amount.signum()<0){
            throw new IllegalArgumentException("Amount must not be negative");
        }

        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public Money add(Money other){
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other){
        return new Money(amount.subtract(other.amount));
    }

    public Money multiply(UnitCount quantity){
        Objects.requireNonNull(quantity, "Quantity must not be null");

        return new Money(amount.multiply(BigDecimal.valueOf(quantity.value())));
    }
}