package io.github.blairjeon.stockflow.product;

import io.github.blairjeon.stockflow.common.BaseTimeEntity;
import io.github.blairjeon.stockflow.common.Money;
import io.github.blairjeon.stockflow.order.Quantity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "products")
public class Product extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Embedded
    @AttributeOverride(
            name = "amount",
            column = @Column(
                    name = "price",
                    nullable = false,
                    precision = 19,
                    scale = 2
            )
    )
    private Money price;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(
                    name = "stock_quantity",
                    nullable = false
            )
    )
    private StockQuantity stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus productStatus;

    private Product(Long userId, String name, Money price, StockQuantity stockQuantity){
        this.userId = Objects.requireNonNull(userId);
        this.name = Objects.requireNonNull(name);
        this.price = Objects.requireNonNull(price);
        this.stockQuantity = Objects.requireNonNull(stockQuantity);
        this.productStatus = ProductStatus.ACTIVE;
    }

    public static Product create(Long userId, String name, Money price, StockQuantity stockQuantity){
        return new Product(userId, name, price, stockQuantity);
    }

    public void updateName(String name){
        this.name = Objects.requireNonNull(name);
    }

    public void updatePrice(Money price){
        this.price = Objects.requireNonNull(price);
    }

    public void updateStockQuantity(StockQuantity stockQuantity){
        this.stockQuantity = Objects.requireNonNull(stockQuantity);
    }

    public void updateStatus(ProductStatus productStatus){
        this.productStatus = Objects.requireNonNull(productStatus);
    }

    public void increaseStockQuantity(Quantity quantity){
        this.stockQuantity = this.stockQuantity.add(quantity);
    }

    public void decreaseStockQuantity(Quantity quantity){
        this.stockQuantity = this.stockQuantity.subtract(quantity);
    }
}