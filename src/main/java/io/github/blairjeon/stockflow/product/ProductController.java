package io.github.blairjeon.stockflow.product;

import io.github.blairjeon.stockflow.product.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public ProductDetailResponse create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ProductCreateRequest request
    ){
        return productService.create(userId, request);
    }

    @GetMapping("/{productId}")
    public ProductDetailResponse getProduct(@PathVariable Long productId){
        return productService.getProduct(productId);
    }

    @GetMapping
    public Slice<ProductSummaryResponse> getProducts(
            @PageableDefault(
                    sort = {"updatedAt", "id"},
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ){
        return productService.getProducts(pageable);
    }

    @PatchMapping("/{productId}")
    public void update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId,
            @Valid@RequestBody ProductUpdateRequest request
    ){
        productService.update(userId, productId, request);
    }

    @PatchMapping("/{productId}/adjust")
    public void adjustStock(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId,
            @Valid@RequestBody InventoryAdjustmentRequest request
    ){
        productService.adjustStock(userId, productId, request);
    }

    @PatchMapping("/{productId}/inventory")
    public void updateInventory(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId,
            @Valid @RequestBody InventoryUpdateRequest request
    ){
        productService.updateInventory(userId, productId, request);
    }

    @DeleteMapping("/{productId}")
    public void delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId
    ){
        productService.delete(userId, productId);
    }
}