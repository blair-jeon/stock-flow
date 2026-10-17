package io.github.blairjeon.stockflow.product.repository;

import io.github.blairjeon.stockflow.order.entity.OrderStatus;
import io.github.blairjeon.stockflow.product.entity.Inventory;
import io.github.blairjeon.stockflow.product.entity.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    @Query("""
    SELECT i
        FROM Inventory i
            JOIN Product p ON p.id = i.productId
                WHERE i.productId = :productId
                    AND p.userId = :userId
    """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findWithLockByUserIdAndProductId(
            @Param("userId") Long userId,
            @Param("productId") Long productId
    );


    @Modifying
    @Query("""
    UPDATE Inventory i
        SET i.stockQuantity = i.stockQuantity + :quantity,
            i.updatedAt = CURRENT_TIMESTAMP
                WHERE i.productId = :productId
                    AND i.stockQuantity + :quantity >= 0
                        AND EXISTS (
                            SELECT p.id
                                FROM Product p
                                    WHERE p.id = i.productId
                                        AND p.userId = :userId
                        )
    """)
    int adjust(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );

    @Modifying
    @Query("""
    UPDATE Inventory i
        SET i.stockQuantity = i.stockQuantity - :quantity
            WHERE i.productId = :productId
                AND i.stockQuantity >= :quantity
                    AND EXISTS (
                        SELECT p.id
                            FROM Product p
                                WHERE p.id = :productId
                                    AND p.unitPrice = :unitPrice
                                        AND p.status = :status
                    )
    """)
    int decrease(
            @Param("productId") Long productId,
            @Param("unitPrice") BigDecimal unitPrice,
            @Param("quantity") int quantity,
            @Param("status") ProductStatus status
    );

    @Modifying
    @Query("""
    UPDATE Inventory i
        SET i.stockQuantity = i.stockQuantity + :quantity
            WHERE i.productId = :productId
    """)
    int increase(
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );
}
