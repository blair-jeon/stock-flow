package io.github.blairjeon.stockflow.order;

import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.common.error.ErrorCode;
import io.github.blairjeon.stockflow.order.dto.OrderCreateRequest;
import io.github.blairjeon.stockflow.order.dto.OrderCreateResponse;
import io.github.blairjeon.stockflow.order.dto.OrderItemRequest;
import io.github.blairjeon.stockflow.order.entity.Order;
import io.github.blairjeon.stockflow.order.entity.OrderStatus;
import io.github.blairjeon.stockflow.order.repository.OrderItemRepository;
import io.github.blairjeon.stockflow.order.repository.OrderRepository;
import io.github.blairjeon.stockflow.product.entity.Inventory;
import io.github.blairjeon.stockflow.product.entity.Product;
import io.github.blairjeon.stockflow.product.repository.InventoryRepository;
import io.github.blairjeon.stockflow.product.repository.ProductRepository;
import io.github.blairjeon.stockflow.user.entity.User;
import io.github.blairjeon.stockflow.user.repository.UserRepository;
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

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrderConcurrencyTest {

    @Container
    @ServiceConnection
    private static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private User user;
    private Product product;
    private Inventory inventory;


    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp(){
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        inventoryRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        user = userRepository.saveAndFlush(
                User.create("test-login-id", "test-password")
        );

        product = productRepository.saveAndFlush(Product.create(
                100L,
                "test-product",
                new BigDecimal("59.99")
        ));

        inventory = inventoryRepository.saveAndFlush(Inventory.create(
                        product.getId(),
                        10
        ));
    }

    @Test
    void shouldDecreaseStockWhenOrderIsCreated(){
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderItemRequest(product.getId(), 1))
        );

        OrderCreateResponse response = orderService.create(user.getId(), request);
        Inventory savedInventory = inventoryRepository.findById(product.getId()).orElseThrow();

        assertAll(
                () -> assertEquals(9, savedInventory.getStockQuantity()),
                () -> assertEquals(OrderStatus.CREATED, response.status())
        );
    }

    @Test
    void shouldNotOverSellWhenOrdersRunConcurrently() throws Exception{
        Long userId = user.getId();

        int requestCount = 100;
        int initialStock = inventory.getStockQuantity();
        long orderBefore = orderRepository.count();

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderItemRequest(product.getId(), 1))
        );

        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(requestCount)) {
            for(int i=0 ; i<requestCount ; i++){
                futures.add(executor.submit(() -> {
                    try {
                        ready.countDown();
                        if(!start.await(10, TimeUnit.SECONDS)){
                            throw new IllegalStateException("Start signal time out.");
                        }

                        orderService.create(userId, request);
                        return true;
                    } catch (BusinessException e) {
                        if(e.getErrorCode() == ErrorCode.NOT_ENOUGH_STOCK){
                            return false;
                        }
                        throw e;
                    }
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            int successCount = 0;
            int rejectCouont = 0;

            for(Future<Boolean> future:futures){
                if(future.get(30, TimeUnit.SECONDS)){
                    successCount++;
                } else {
                    rejectCouont++;
                }
            }

            Inventory savendInventory = inventoryRepository.findById(product.getId()).orElseThrow();

            assertEquals(initialStock, successCount);
            assertEquals(requestCount-successCount, rejectCouont);
            assertEquals(0, savendInventory.getStockQuantity());
            assertEquals(orderBefore+successCount, orderRepository.count());
        }
    }

    @Test
    void shouldRestoreStockOnlyOnceWhenSameOrderCanceled() throws Exception{
        long orderBefore = orderRepository.count();

        int initialStock = inventory.getStockQuantity();
        int requestCount = 100;

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderItemRequest(product.getId(), 1))
        );

        OrderCreateResponse response = orderService.create(user.getId(), request);
        Long orderId = response.orderId();
        Long userId = user.getId();

        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(requestCount)){
            for(int i=0 ; i<requestCount ; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        ready.countDown();

                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Start signal time out.");
                        }

                        orderService.cancel(userId, orderId);
                        return true;
                    } catch (BusinessException e) {
                        if (e.getErrorCode() == ErrorCode.CANNOT_CANCEL_ORDER) {
                            return false;
                        }
                        throw e;
                    }
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            int successCount = 0;
            int rejectCount = 0;

            for(Future<Boolean> future:futures){
                if(future.get(30, TimeUnit.SECONDS)){
                    successCount++;
                } else {
                    rejectCount++;
                }
            }

            Order savedOrder = orderRepository.findById(orderId).orElseThrow();
            Inventory savedInventory = inventoryRepository.findById(product.getId()).orElseThrow();

            assertEquals(orderBefore+successCount, orderRepository.count());
            assertEquals(OrderStatus.CANCELLED, savedOrder.getStatus());
            assertEquals(initialStock-1+successCount, savedInventory.getStockQuantity());
        }
    }
}