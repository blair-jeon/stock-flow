package io.github.blairjeon.stockflow.order;

import io.github.blairjeon.stockflow.common.Money;
import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.common.error.ErrorCode;
import io.github.blairjeon.stockflow.order.dto.OrderCreateRequest;
import io.github.blairjeon.stockflow.order.dto.OrderCreateResponse;
import io.github.blairjeon.stockflow.order.dto.OrderItemRequest;
import io.github.blairjeon.stockflow.order.repository.OrderRepository;
import io.github.blairjeon.stockflow.product.Product;
import io.github.blairjeon.stockflow.product.StockQuantity;
import io.github.blairjeon.stockflow.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class OrderConcurrencyTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;

    private Product product;

    @BeforeEach
    void cleanUp(){
        orderRepository.deleteAll();
        productRepository.deleteAll();

        product = productRepository.saveAndFlush(Product.create(
                1L,
                "concurrency-test-product",
                new Money(new BigDecimal("99.99")),
                new StockQuantity(10)
        ));
    }

    @Test
    void shouldDecreaseStockWhenOrderIsCreated() {
        Long productId = product.getId();
        long orderCountBefore = orderRepository.count();

        OrderCreateRequest request =
                new OrderCreateRequest(List.of(new OrderItemRequest(productId, 1)));

        orderService.create(2L, request);

        Product savedProduct = productRepository.findById(productId)
                .orElseThrow();

        assertEquals(9, savedProduct.getStockQuantity().value());
        assertEquals(orderCountBefore + 1, orderRepository.count());
    }

    @Test
    void shouldNotOversellWhenOrdersRunConcurrently() throws Exception {
        int initialStock = 10;
        int requestCount = 100;

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(requestCount);
        List<Future<Boolean>> futures = new ArrayList<>();

        long orderBefore = orderRepository.count();

        OrderCreateRequest request =
                new OrderCreateRequest(List.of(new OrderItemRequest(product.getId(), 1)));

        try (ExecutorService executor = Executors.newFixedThreadPool(requestCount)) {
            try {
                for (int i = 0; i < requestCount; i++) {
                    futures.add(executor.submit(() -> {
                        ready.countDown();

                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Start signal time out");
                        }

                        try {
                            orderService.create(2L, request);
                            return true;
                        } catch (BusinessException e) {
                            if (e.getErrorCode() == ErrorCode.NOT_ENOUGH_STOCK
                                    || e.getErrorCode() == ErrorCode.ORDER_CONFLICT) {
                                return false;
                            }
                            throw e;
                        }
                    }));
                }

                assertTrue(ready.await(10, TimeUnit.SECONDS));
                start.countDown();

                int successCount = 0;
                int rejectedCount = 0;

                for (Future<Boolean> future : futures) {
                    if (future.get(30, TimeUnit.SECONDS)) {
                        successCount++;
                    } else {
                        rejectedCount++;
                    }
                }

                Product savedProduct = productRepository.findById(product.getId()).orElseThrow();
                assertEquals(initialStock, successCount);
                assertEquals(requestCount - initialStock, rejectedCount);
                assertEquals(0, savedProduct.getStockQuantity().value());
                assertEquals(orderBefore + successCount, orderRepository.count());

            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void shouldRestoreStockOnlyOnceWhenSameOrderIsCanceledConcurrently() throws Exception{
        int initialStock = product.getStockQuantity().value();
        int requestCount = 10;

        OrderCreateRequest request = new OrderCreateRequest(List.of(new OrderItemRequest(product.getId(), 1)));

        OrderCreateResponse response = orderService.create(1L, request);
        Long orderId = response.orderId();

        Product productAfterOrder = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(initialStock-1, productAfterOrder.getStockQuantity().value());

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(requestCount);
        List<Future<Boolean>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(requestCount)) {
            try {
                for(int i=0 ; i<requestCount ; i++){
                    futures.add(executor.submit(() -> {
                        ready.countDown();

                        if(!start.await(10, TimeUnit.SECONDS)){
                            throw new IllegalStateException("Start signal time out");
                        }

                        try{
                            orderService.cancel(1L, orderId);
                            return true;
                        } catch (BusinessException e) {
                            if(e.getErrorCode() == ErrorCode.CANNOT_CANCEL_ORDER){
                                return false;
                            }

                            throw e;
                        }
                    }));
                }

                int successCount = 0;
                int rejectedCount = 0;

                assertTrue(ready.await(10, TimeUnit.SECONDS));
                start.countDown();

                for(Future<Boolean> future:futures){
                    if(future.get(30, TimeUnit.SECONDS)){
                        successCount++;
                    } else {
                        rejectedCount++;
                    }
                }

                Product productAfterCancel = productRepository.findById(product.getId()).orElseThrow();

                assertEquals(requestCount, successCount+rejectedCount);
                assertEquals(initialStock, productAfterCancel.getStockQuantity().value());
            } finally {
                executor.shutdownNow();
            }
        }
    }
}