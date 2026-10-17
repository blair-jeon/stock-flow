package io.github.blairjeon.stockflow.order.entity;

import io.github.blairjeon.stockflow.product.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Getter
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Column(nullable = false, updatable = false)
    private Long productId;

    @Column(nullable = false, updatable = false)
    private String productName;

    @DecimalMin("0.00")
    @Column(nullable = false, updatable = false)
    private BigDecimal unitPrice;

    @Positive
    @Column(nullable = false, updatable = false)
    private int quantity;

    private OrderItem(Order order, Product product, int quantity){
        Objects.requireNonNull(product);

        this.order = Objects.requireNonNull(order);
        this.productId = product.getId();
        this.productName = product.getName();
        this.unitPrice = product.getUnitPrice();
        this.quantity = quantity;
    }

    public static OrderItem create(Order order, Product product, int quantity){
        return new OrderItem(order, product, quantity);
    }
}