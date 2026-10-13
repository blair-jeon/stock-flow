package io.github.blairjeon.stockflow.order;

import io.github.blairjeon.stockflow.common.BaseTimeEntity;
import io.github.blairjeon.stockflow.common.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Embedded
    @AttributeOverride(
            name = "amount",
            column = @Column(
                    name = "total_amount",
                    nullable = false,
                    updatable = false,
                    precision = 19,
                    scale = 2
            )
    )
    private Money totalAmount = new Money(BigDecimal.ZERO);

    @Column(nullable = false, updatable = false)
    private String representativeProductName;

    @Column(nullable = false, updatable = false)
    private int lineItemCount = 0;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Order(Long userId) {
        this.userId = Objects.requireNonNull(userId);
        this.status = OrderStatus.PENDING;
    }

    public static Order create(Long userId) {
        return new Order(userId);
    }

    public void addItem(OrderItem orderItem){
        this.totalAmount =
                this.totalAmount.add(orderItem.getUnitPrice().multiply(orderItem.getQuantity()));
        if(lineItemCount==0) {
            this.representativeProductName = orderItem.getProductName();
        }
        lineItemCount++;
    }

    public void cancel(){
        // Status 체크해야함, 중복취소 현재 가능
        this.status = OrderStatus.CANCELED;
    }
}
