package io.github.blairjeon.stockflow.product;

import io.github.blairjeon.stockflow.ErrorCode;
import io.github.blairjeon.stockflow.common.Money;
import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.product.dto.ProductCreateRequest;
import io.github.blairjeon.stockflow.product.dto.ProductResponse;
import io.github.blairjeon.stockflow.product.dto.ProductUpdateRequest;
import io.github.blairjeon.stockflow.product.repository.ProductRepository;
import io.github.blairjeon.stockflow.user.UserRole;
import io.github.blairjeon.stockflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void create(Long userId, ProductCreateRequest request){
        if(!userRepository.existsByIdAndUserRole(userId, UserRole.SELLER)) {
            throw new BusinessException(ErrorCode.SELLER_ONLY);
        }

        Product product = Product.create(
                userId,
                request.name(),
                new Money(request.price()),
                new StockQuantity(request.stockQuantity())
        );

        productRepository.save(product);
    }

    public ProductResponse getProduct(Long productId){
        Product product = productRepository.findById(productId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductResponse.from(product);
    }

    public Slice<ProductResponse> getProducts(Pageable pageable){
        return productRepository.findAllBy(pageable)
                .map(ProductResponse::from);
    }

    @Transactional
    public void update(Long userId, Long productId, ProductUpdateRequest request){
        Product product = getProductByUserIdAndId(userId, productId);

        if(request.name()!=null){
            product.updateName(request.name());
        }

        if(request.price()!=null){
            product.updatePrice(new Money(request.price()));
        }

        if(request.stockQuantity()!=null){
            product.updateStockQuantity(new StockQuantity(request.stockQuantity()));
        }

        if(request.productStatus()!=null){
            product.updateStatus(request.productStatus());
        }

        productRepository.save(product);
    }

    @Transactional
    public void delete(Long userId, Long productId){
        Product product = getProductByUserIdAndId(userId, productId);

        product.updateStockQuantity(new StockQuantity(0));
        product.updateStatus(ProductStatus.DISCONTINUED);
    }

    private Product getProductByUserIdAndId(Long userId, Long productId){
        return productRepository.findByUserIdAndId(userId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}