package io.github.blairjeon.stockflow.order;

import io.github.blairjeon.stockflow.common.Money;
import io.github.blairjeon.stockflow.order.dto.OrderCreateRequest;
import io.github.blairjeon.stockflow.order.dto.OrderItemRequest;
import io.github.blairjeon.stockflow.order.exception.DuplicateOrderItemException;
import io.github.blairjeon.stockflow.order.exception.OrderPermissionDeniedException;
import io.github.blairjeon.stockflow.order.repository.OrderItemRepository;
import io.github.blairjeon.stockflow.order.repository.OrderRepository;
import io.github.blairjeon.stockflow.product.Product;
import io.github.blairjeon.stockflow.product.ProductStatus;
import io.github.blairjeon.stockflow.product.exception.ProductNotFoundException;
import io.github.blairjeon.stockflow.product.repository.ProductRepository;
import io.github.blairjeon.stockflow.product.StockQuantity;
import io.github.blairjeon.stockflow.user.repository.UserRepository;
import io.github.blairjeon.stockflow.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest{
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @InjectMocks private OrderService orderService;
    @Captor ArgumentCaptor<Order> orderCaptor;
    @Captor ArgumentCaptor<List<OrderItem>> itemCaptor;

    private Product product(Long id, String name, String price, int stock){
        Product product =
                Product.create(1L, name, new Money(new BigDecimal(price)), new StockQuantity(stock));
        ReflectionTestUtils.setField(product, "id", id);

        return product;
    }

    private List<Product> existingProducts(){
        return List.of(
                product(10L, "Keyboard", "99.99", 10),
                product(20L, "Mouse", "39.99", 5)
        );
    }

    @Test
    void shouldCreateOrderWhenRequestIsValid(){
        List<Product> products = existingProducts();
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(
                        new OrderItemRequest(10L, 2),
                        new OrderItemRequest(20L, 4)
                )
        );

        when(userRepository.existsByIdAndUserRole(1L, UserRole.USER))
                .thenReturn(true);
        when(productRepository.findAllByIdInAndProductStatus(List.of(10L, 20L), ProductStatus.ACTIVE))
                .thenReturn(products);

        orderService.create(1L, request);

        verify(userRepository).existsByIdAndUserRole(1L, UserRole.USER);
        verify(productRepository).findAllById(List.of(10L, 20L));
        verify(orderRepository).save(orderCaptor.capture());
        verify(orderItemRepository).saveAll(itemCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        List<OrderItem> savedItems = itemCaptor.getValue();

        assertEquals(1L, savedOrder.getUserId());
        assertEquals(2, savedOrder.getLineItemCount());
        assertEquals(new BigDecimal("359.94"), savedOrder.getTotalAmount().amount());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());

        assertEquals(2, savedItems.size());

        assertEquals(10L, savedItems.getFirst().getProductId());
        assertEquals("Keyboard", savedItems.getFirst().getProductName());
        assertEquals(new BigDecimal("99.99"), savedItems.getFirst().getUnitPrice().amount());
        assertEquals(new Quantity(2), savedItems.getFirst().getQuantity());

        assertEquals(20L, savedItems.get(1).getProductId());
        assertEquals("Mouse", savedItems.get(1).getProductName());
        assertEquals(new BigDecimal("39.99"), savedItems.get(1).getUnitPrice().amount());
        assertEquals(new Quantity(4), savedItems.get(1).getQuantity());
    }

    @Test
    void shouldRejectOrderWhenUserIsNotAuthorized(){
        when(userRepository.existsByIdAndUserRole(1L, UserRole.USER))
                .thenReturn(false);

        assertThrows(OrderPermissionDeniedException.class,
                () -> orderService.create(1L, null));

        verifyNoInteractions(productRepository);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void shouldRejectOrderWhenDuplicateProductsExists(){
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(
                        new OrderItemRequest(10L, 2),
                        new OrderItemRequest(10L, 4)
                )
        );

        when(userRepository.existsByIdAndUserRole(1L, UserRole.USER))
                .thenReturn(true);

        assertThrows(DuplicateOrderItemException.class,
                () -> orderService.create(1L, request));

        verifyNoInteractions(productRepository);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void shouldRejectOrderWhenProductDoesNotExists(){
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(
                        new OrderItemRequest(10L, 2),
                        new OrderItemRequest(20L, 4)
                )
        );

        when(userRepository.existsByIdAndUserRole(1L, UserRole.USER))
                .thenReturn(true);
        when(productRepository.findAllByIdInAndProductStatus(List.of(10L, 20L), ProductStatus.ACTIVE))
                .thenReturn(List.of(product(10L, "Keyboard", "99.99", 10)));

        assertThrows(ProductNotFoundException.class,
                () -> orderService.create(1L, request));

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void shouldRejectOrderWhenStockIsInsufficient(){
        List<Product> products = existingProducts();
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(
                        new OrderItemRequest(10L, 2),
                        new OrderItemRequest(20L, 999)
                )
        );

        when(userRepository.existsByIdAndUserRole(1L, UserRole.USER))
                .thenReturn(true);
        when(productRepository.findAllByIdInAndProductStatus(List.of(10L, 20L), ProductStatus.ACTIVE))
                .thenReturn(products);

        assertThrows(IllegalArgumentException.class,
                () -> orderService.create(1L, request));

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderItemRepository);
    }
}