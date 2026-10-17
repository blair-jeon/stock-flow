package io.github.blairjeon.stockflow.product.entity;

import io.github.blairjeon.stockflow.common.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Getter
@Table(name = "inventories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseTimeEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private Long productId;

    @PositiveOrZero
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    private Inventory(Long productId, int stockQuantity){
        this.productId = Objects.requireNonNull(productId);
        this.stockQuantity = stockQuantity;
    }

    public static Inventory create(Long productId, int stockQuantity){
        return new Inventory(productId, stockQuantity);
    }

    public void updateStock(int stockQuantity){
        this.stockQuantity = stockQuantity;
    }
}