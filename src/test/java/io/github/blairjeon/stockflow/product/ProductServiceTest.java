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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProductWhenUserIsSeller(){
        ProductCreateRequest request = new ProductCreateRequest(
                "test-product-name",
                new BigDecimal("99.99"),
                100
        );
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(userRepository.existsByIdAndUserRole(1L, UserRole.SELLER))
                .thenReturn(true);

        productService.create(1L, request);

        verify(userRepository).existsByIdAndUserRole(1L, UserRole.SELLER);
        verify(productRepository).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();
        assertEquals(1L, savedProduct.getUserId());
        assertEquals("test-product-name", savedProduct.getName());
        assertEquals(new BigDecimal("99.99"), savedProduct.getPrice().amount());
        assertEquals(100, savedProduct.getStockQuantity().value());
    }

    @Test
    void shouldRejectProductCreationWhenUserIsNotSeller(){
        ProductCreateRequest request = new ProductCreateRequest(
                "test-product-name",
                new BigDecimal("99.99"),
                100
        );

        when(userRepository.existsByIdAndUserRole(1L, UserRole.SELLER))
                .thenReturn(false);

        assertThrows(SellerRequiredException.class,
                () -> productService.create(1L, request));

        verify(userRepository).existsByIdAndUserRole(1L, UserRole.SELLER);
        verifyNoInteractions(productRepository);
    }

    @Test
    void shouldReturnProductWhenProductExists(){
        Product product = Product.create(
                1L,
                "test-product-name",
                new Money(new BigDecimal("99.99")),
                new StockQuantity(100)
        );
        ReflectionTestUtils.setField(product, "id", 10L);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        ProductResponse result = productService.getProduct(10L);

        verify(productRepository).findById(10L);

        assertEquals(10L, result.id());
        assertEquals("test-product-name", result.name());
        assertEquals(new BigDecimal("99.99"), result.price());
        assertEquals(100, result.stockQuantity());
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound(){
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.getProduct(10L));

        verify(productRepository).findById(10L);
    }

    @Test
    void shouldReturnProductSliceWhenProductsExist() {
        Product product1 = Product.create(
                1L,
                "product-1",
                new Money(new BigDecimal("99.99")),
                new StockQuantity(100)
        );
        Product product2 = Product.create(
                1L,
                "product-2",
                new Money(new BigDecimal("49.99")),
                new StockQuantity(50)
        );
        ReflectionTestUtils.setField(product1, "id", 10L);
        ReflectionTestUtils.setField(product2, "id", 11L);

        Pageable pageable = PageRequest.of(0, 20);
        Slice<Product> slice = new SliceImpl<>(
                List.of(product1, product2),
                pageable,
                false
        );

        when(productRepository.findAllBy(pageable)).thenReturn(slice);

        Slice<ProductResponse> result = productService.getProducts(pageable);

        verify(productRepository).findAllBy(pageable);
        assertEquals(2, result.getContent().size());

        assertEquals(10L, result.getContent().getFirst().id());
        assertEquals("product-1", result.getContent().getFirst().name());
        assertEquals(new BigDecimal("99.99"), result.getContent().get(0).price());
        assertEquals(100, result.getContent().get(0).stockQuantity());

        assertEquals(11L, result.getContent().get(1).id());
        assertEquals("product-2", result.getContent().get(1).name());
        assertEquals(new BigDecimal("49.99"), result.getContent().get(1).price());
        assertEquals(50, result.getContent().get(1).stockQuantity());

        assertFalse(result.hasNext());
    }

    @Test
    void shouldReturnEmptySliceWhenNoProductsExist() {
        Pageable pageable = PageRequest.of(0, 20);
        Slice<Product> slice = new SliceImpl<>(
                List.of(),
                pageable,
                false
        );

        when(productRepository.findAllBy(pageable)).thenReturn(slice);

        Slice<ProductResponse> result = productService.getProducts(pageable);

        verify(productRepository).findAllBy(pageable);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void shouldUpdateProductWhenUserIsOwner(){
        Product product = Product.create(
                1L,
                "test-product-name",
                new Money(new BigDecimal("99.99")),
                new StockQuantity(100)
        );
        ReflectionTestUtils.setField(product, "id", 10L);

        ProductUpdateRequest request = new ProductUpdateRequest(
                "update-product-name",
                new BigDecimal("111.11"),
                200,
                ProductStatus.INACTIVE
        );

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(productRepository.findByUserIdAndId(1L, 10L))
                .thenReturn(Optional.of(product));

        productService.update(1L, 10L, request);

        verify(productRepository).findByUserIdAndId(1L, 10L);
        verify(productRepository).save(productCaptor.capture());

        Product result = productCaptor.getValue();
        assertEquals("update-product-name", result.getName());
        assertEquals(new BigDecimal("111.11"), result.getPrice().amount());
        assertEquals(200, result.getStockQuantity().value());
        assertEquals(ProductStatus.INACTIVE, result.getProductStatus());
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotOwner(){
        ProductUpdateRequest request = new ProductUpdateRequest(null, null, null, null);

        when(productRepository.findByUserIdAndId(1L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.update(1L, 10L, request));

        verify(productRepository).findByUserIdAndId(1L, 10L);
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldDeleteProductWhenUserIsOwner(){
        Product product = Product.create(
                1L,
                "test-product-name",
                new Money(new BigDecimal("99.99")),
                new StockQuantity(100)
        );
        ReflectionTestUtils.setField(product, "id", 10L);

        when(productRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(product));

        productService.delete(1L, 10L);

        verify(productRepository).findByUserIdAndId(1L, 10L);
        verify(productRepository).delete(product);
    }

    @Test
    void shouldThrowExceptionWhenDeletingProductNotOwnedByUser(){
        when(productRepository.findByUserIdAndId(1L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.delete(1L, 10L));

        verify(productRepository).findByUserIdAndId(1L, 10L);
        verify(productRepository, never()).delete(any());
    }
}