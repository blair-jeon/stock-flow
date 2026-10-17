package io.github.blairjeon.stockflow.order.entity;

import io.github.blairjeon.stockflow.common.BaseTimeEntity;
import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.common.error.ErrorCode;
import io.github.blairjeon.stockflow.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    private int lineCount;

    private Order(Long userId){
        this.userId = Objects.requireNonNull(userId);
        this.status = OrderStatus.CREATED;
        this.amount = BigDecimal.ZERO;
    }

    public static Order create(Long userId){
        return new Order(userId);
    }

    public void addItem(OrderItem item){
        this.amount = this.amount.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

        if(lineCount==0){
            this.name = item.getProductName();
        }
        lineCount++;
    }

    public void cancel(){
        if(!this.status.canCancel()){
            throw new BusinessException(ErrorCode.CANNOT_CANCEL_ORDER);
        }

        this.status = OrderStatus.CANCELLED;
    }

}