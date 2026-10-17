package io.github.blairjeon.stockflow.product.entity;

import io.github.blairjeon.stockflow.common.BaseTimeEntity;
import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.common.error.ErrorCode;
import io.github.blairjeon.stockflow.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Getter
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Long userId;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 17, fraction = 2)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    private Product(Long userId, String name, BigDecimal unitPrice) {
        this.userId = Objects.requireNonNull(userId);
        this.name = name;
        this.unitPrice = unitPrice;
        this.status = ProductStatus.ACTIVE;
    }

    public static Product create(Long userId, String name, BigDecimal unitPrice){
        return new Product(userId, name, unitPrice);
    }

    public void update(String name, BigDecimal unitPrice, ProductStatus status){
        if(name!=null){
            if (name.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            this.name = name;
        }
        if(unitPrice!=null){ this.unitPrice = unitPrice; }
        if(status!=null){ this.status = status; }
    }
}