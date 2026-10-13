package io.github.blairjeon.stockflow.product.repository;

import io.github.blairjeon.stockflow.product.Product;
import io.github.blairjeon.stockflow.product.ProductStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Slice<Product> findAllBy(Pageable pageable);
    Optional<Product> findByUserIdAndId(Long userId, Long id);
    List<Product> findAllByIdInAndProductStatus(List<Long> productId, ProductStatus productStatus);
}
