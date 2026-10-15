package io.github.blairjeon.stockflow.product.repository;

import io.github.blairjeon.stockflow.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Slice<Product> findAllBy(Pageable pageable);
    Optional<Product> findByUserIdAndId(Long userId, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p from Product p WHERE p.userId=:userId AND p.id=:productId")
    Optional<Product> findWithLockByUserIdAndId(@Param("userId") Long userId, @Param("productId") Long id);

    @Modifying
    @Query(value = """
    UPDATE products
    SET stock_quantity = stock_quantity - :quantity
    WHERE id = :productId
    AND stock_quantity >= :quantity
    AND status = 'ACTIVE'
    AND price = :expectedPrice
    """, nativeQuery = true)
    int decreaseStock(
            @Param("productId") Long productId,
            @Param("quantity") int quantity,
            @Param("expectedPrice") BigDecimal expectedPrice
    );

    @Modifying
    @Query(value = """
    UPDATE products
    SET stock_quantity = stock_quantity + :quantity
    WHERE id = :productId
    """, nativeQuery = true)
    int increaseStock(
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );
}