package io.github.blairjeon.stockflow.order;

import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.common.error.ErrorCode;
import io.github.blairjeon.stockflow.order.dto.*;
import io.github.blairjeon.stockflow.order.entity.Order;
import io.github.blairjeon.stockflow.order.entity.OrderItem;
import io.github.blairjeon.stockflow.order.repository.OrderItemRepository;
import io.github.blairjeon.stockflow.order.repository.OrderRepository;
import io.github.blairjeon.stockflow.product.entity.Product;
import io.github.blairjeon.stockflow.product.entity.ProductStatus;
import io.github.blairjeon.stockflow.product.repository.InventoryRepository;
import io.github.blairjeon.stockflow.product.repository.ProductRepository;
import io.github.blairjeon.stockflow.user.entity.UserStatus;
import io.github.blairjeon.stockflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderCreateResponse create(Long userId, OrderCreateRequest request){
        if(!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)){
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }

        List<OrderItemRequest> sortedRequest = request.items().stream()
                .sorted(Comparator.comparing(OrderItemRequest::productId))
                .toList();

        List<Long> productIds = sortedRequest.stream()
                .map(OrderItemRequest::productId)
                .distinct()
                .toList();

        if(sortedRequest.size()!=productIds.size()){
            throw new BusinessException(ErrorCode.DUPLICATE_ORDER_ITEM);
        }

        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        Function.identity()
                ));

        Order order = Order.create(userId);
        List<OrderItem> items = new ArrayList<>();

        for(OrderItemRequest itemRequest:sortedRequest){
            Product product = productMap.get(itemRequest.productId());

            if(product==null){
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }

            int successCount = inventoryRepository.decrease(
                    product.getId(),
                    product.getUnitPrice(),
                    itemRequest.quantity(),
                    ProductStatus.ACTIVE
            );

            if(successCount!=1){
                throw new BusinessException(ErrorCode.NOT_ENOUGH_STOCK);
            }

            OrderItem orderItem = OrderItem.create(order, product, itemRequest.quantity());
            items.add(orderItem);

            order.addItem(orderItem);
        }

        orderRepository.save(order);
        orderItemRepository.saveAll(items);

        return OrderCreateResponse.from(order);
    }

    @Transactional
    public OrderCancelResponse cancel(Long userId, Long orderId){
        Order order = orderRepository.findWithLockByUserIdAndId(userId, orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.cancel();

        List<OrderItem> items = orderItemRepository.findAllByOrder(order);

        for(OrderItem item:items){
            int successCount = inventoryRepository.increase(item.getProductId(), item.getQuantity());

            if(successCount!=1){
                throw new BusinessException(ErrorCode.NOT_ENOUGH_STOCK);
            }
        }

        return OrderCancelResponse.from(order);
    }

    public OrderResponse getOrder(Long userId, Long orderId){
        Order order = orderRepository.findByUserIdAndId(userId, orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderItemResponse> items = orderItemRepository.findAllByOrder(order)
                .stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.from(order, items);
    }

    public List<OrderDetailResponse> getOrders(Long userId, Pageable pageable){
        return orderRepository.findAllByUserId(userId, pageable)
                .stream()
                .map(OrderDetailResponse::from)
                .toList();
    }
}