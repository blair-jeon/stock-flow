package io.github.blairjeon.stockflow.product.repository;

import io.github.blairjeon.stockflow.order.Quantity;
import io.github.blairjeon.stockflow.product.Product;
import io.github.blairjeon.stockflow.product.ProductStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Slice<Product> findAllBy(Pageable pageable);
    Optional<Product> findByUserIdAndId(Long userId, Long id);
    List<Product> findAllByIdInAndProductStatus(List<Long> productId, ProductStatus productStatus);

    @Modifying
    @Query(value = """
    UPDATE products
    SET stock_quantity = stock_quantity - :quantity
    WHERE id = :productId
    AND stock_quantity >= :quantity
    AND product_status = 'ACTIVE'
    AND unit_price = :expectedPrice
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