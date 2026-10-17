package io.github.blairjeon.stockflow.product;

import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.common.error.ErrorCode;
import io.github.blairjeon.stockflow.product.dto.*;
import io.github.blairjeon.stockflow.product.entity.Inventory;
import io.github.blairjeon.stockflow.product.entity.Product;
import io.github.blairjeon.stockflow.product.entity.ProductStatus;
import io.github.blairjeon.stockflow.product.repository.InventoryRepository;
import io.github.blairjeon.stockflow.product.repository.ProductRepository;
import io.github.blairjeon.stockflow.user.entity.User;
import io.github.blairjeon.stockflow.user.entity.UserRole;
import io.github.blairjeon.stockflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public ProductDetailResponse create(Long userId, ProductCreateRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(!user.getRole().equals(UserRole.SELLER)){
            throw new BusinessException(ErrorCode.SELLER_ONLY);
        }

        Product product = Product.create(
                user.getId(),
                request.name(),
                request.unitPrice()
        );
        Product savedProduct = productRepository.save(product);

        Inventory inventory = Inventory.create(
                product.getId(),
                request.stock()
        );
        Inventory savedInventory = inventoryRepository.save(inventory);

        return ProductDetailResponse.from(user, savedProduct, savedInventory);
    }

    public ProductDetailResponse getProduct(Long productId){
        return productRepository.findDetailById(productId, List.of(ProductStatus.ACTIVE, ProductStatus.SUSPENDED))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public Slice<ProductSummaryResponse> getProducts(Pageable pageable){
        return productRepository.findProducts(
                List.of(ProductStatus.ACTIVE, ProductStatus.SUSPENDED),
                pageable
        );
    }

    @Transactional
    public void update(Long userId, Long productId, ProductUpdateRequest request){
        Product product = productRepository.findWithLockByUserIdAndId(userId, productId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.update(request.name(), request.unitPrice(), request.status());
    }

    @Transactional
    public void adjustStock(Long userId, Long productId, InventoryAdjustmentRequest request){
        int updated = inventoryRepository.adjust(userId, productId, request.adjustment());

        if(updated!=1){
            throw new BusinessException(ErrorCode.STOCK_ADJUSTMENT_FAILED);
        }
    }

    @Transactional
    public void updateInventory(Long userId, Long productId, InventoryUpdateRequest request){
        Inventory inventory = inventoryRepository.findWithLockByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        inventory.updateStock(request.stockQuantity());
    }

    @Transactional
    public void delete(Long userId, Long productId){
        Product product = productRepository.findByUserIdAndId(userId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        inventoryRepository.deleteById(product.getId());
        productRepository.delete(product);
    }
}