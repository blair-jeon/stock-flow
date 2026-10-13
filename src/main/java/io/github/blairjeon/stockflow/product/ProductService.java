package io.github.blairjeon.stockflow.product;

import io.github.blairjeon.stockflow.common.Money;
import io.github.blairjeon.stockflow.product.dto.ProductCreateRequest;
import io.github.blairjeon.stockflow.product.dto.ProductResponse;
import io.github.blairjeon.stockflow.product.dto.ProductUpdateRequest;
import io.github.blairjeon.stockflow.product.exception.ProductNotFoundException;
import io.github.blairjeon.stockflow.product.exception.SellerRequiredException;
import io.github.blairjeon.stockflow.product.repository.ProductRepository;
import io.github.blairjeon.stockflow.user.repository.UserRepository;
import io.github.blairjeon.stockflow.user.UserRole;
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
            throw new SellerRequiredException();
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
                .orElseThrow(ProductNotFoundException::new);
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
        productRepository.delete(product);
    }

    private Product getProductByUserIdAndId(Long userId, Long productId){
        return productRepository.findByUserIdAndId(userId, productId)
                .orElseThrow(ProductNotFoundException::new);
    }
}