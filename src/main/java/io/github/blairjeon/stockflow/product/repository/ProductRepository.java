package io.github.blairjeon.stockflow.product.repository;

import io.github.blairjeon.stockflow.product.dto.ProductDetailResponse;
import io.github.blairjeon.stockflow.product.dto.ProductSummaryResponse;
import io.github.blairjeon.stockflow.product.entity.Product;
import io.github.blairjeon.stockflow.product.entity.ProductStatus;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findWithLockByUserIdAndId(Long userId, Long id);

    Optional<Product> findByUserIdAndId(Long userId, Long productId);

    @Query("""
    SELECT new io.github.blairjeon.stockflow.product.dto.ProductDetailResponse(
        p.id,
        u.id,
        u.loginId,
        p.name,
        p.unitPrice,
        p.status,
        i.stockQuantity
    )
        FROM Product p
            JOIN User u ON u.id = p.userId
                JOIN Inventory i ON i.productId = p.id
                    WHERE p.id = :productId
                        AND p.status IN :statuses
    """)
    Optional<ProductDetailResponse> findDetailById(
            @Param("productId") Long productId,
            @Param("statuses")Collection<ProductStatus> statuses
    );

    @Query("""
    SELECT new io.github.blairjeon.stockflow.product.dto.ProductSummaryResponse(
        p.id,
        u.id,
        u.loginId,
        p.name,
        p.unitPrice,
        p.status
    )
        FROM Product p
            JOIN User u ON u.id = p.userId
                WHERE p.status IN :statuses
    """)
    Slice<ProductSummaryResponse> findProducts(
            @Param("statuses") Collection<ProductStatus> statuses,
            Pageable pageable
    );

}