package io.github.blairjeon.stockflow.order;

import io.github.blairjeon.stockflow.common.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Column(nullable = false, updatable = false)
    private Long productId;

    @Column(nullable = false, updatable = false)
    private String productName;

    @Embedded
    @AttributeOverride(
            name = "amount",
            column = @Column(
                    name = "unit_price",
                    nullable = false,
                    updatable = false,
                    precision = 19,
                    scale = 2
            )
    )
    private Money unitPrice;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(
                    name = "quantity",
                    nullable = false,
                    updatable = false
            )
    )
    private Quantity quantity;

    private OrderItem(Order order, Long productId, String productName, Money unitPrice, Quantity quantity
    ) {
        this.order = Objects.requireNonNull(order);
        this.productId = Objects.requireNonNull(productId);
        this.productName = Objects.requireNonNull(productName);
        this.unitPrice = Objects.requireNonNull(unitPrice);
        this.quantity = Objects.requireNonNull(quantity);
    }

    public static OrderItem create(
            Order order,
            Long productId,
            String productName,
            Money unitPrice,
            Quantity quantity
    ) {
        return new OrderItem(order, productId, productName, unitPrice, quantity);
    }
}