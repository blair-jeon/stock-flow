package io.github.blairjeon.stockflow.product;

import io.github.blairjeon.stockflow.product.dto.ProductCreateRequest;
import io.github.blairjeon.stockflow.product.dto.ProductResponse;
import io.github.blairjeon.stockflow.product.dto.ProductUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ProductCreateRequest request
    ){
        productService.create(userId, request);
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable Long productId){
        return productService.getProduct(productId);
    }

    @GetMapping
    public Slice<ProductResponse> getProducts(Pageable pageable){
        return productService.getProducts(pageable);
    }

    @PatchMapping("/{productId}")
    public void update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request
    ){
        productService.update(userId, productId, request);
    }

    @DeleteMapping("/{productId}")
    public void delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId
    ){
        productService.delete(userId, productId);
    }
}
