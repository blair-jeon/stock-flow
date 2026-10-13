package io.github.blairjeon.stockflow.order;

import io.github.blairjeon.stockflow.order.dto.OrderCreateRequest;
import io.github.blairjeon.stockflow.order.dto.OrderItemRequest;
import io.github.blairjeon.stockflow.order.dto.OrderResponse;
import io.github.blairjeon.stockflow.order.dto.OrderSummaryResponse;
import io.github.blairjeon.stockflow.order.exception.DuplicateOrderItemException;
import io.github.blairjeon.stockflow.order.exception.OrderNotFoundException;
import io.github.blairjeon.stockflow.order.exception.OrderPermissionDeniedException;
import io.github.blairjeon.stockflow.order.repository.OrderItemRepository;
import io.github.blairjeon.stockflow.order.repository.OrderRepository;
import io.github.blairjeon.stockflow.product.Product;
import io.github.blairjeon.stockflow.product.ProductStatus;
import io.github.blairjeon.stockflow.product.exception.ProductNotFoundException;
import io.github.blairjeon.stockflow.product.repository.ProductRepository;
import io.github.blairjeon.stockflow.user.repository.UserRepository;
import io.github.blairjeon.stockflow.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void create(Long userId, OrderCreateRequest request){
        if(!userRepository.existsByIdAndUserRole(userId, UserRole.USER)){
            throw new OrderPermissionDeniedException();
        }

        List<Long> productIds = request.items()
                .stream()
                .map(OrderItemRequest::productId)
                .distinct()
                .sorted()
                .toList();

        if(productIds.size()!=request.items().size()){
            throw new DuplicateOrderItemException();
        }

        Map<Long, Product> productsById = productRepository.findAllByIdInAndProductStatus(productIds, ProductStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        product -> product)
                );

        Order order = Order.create(userId);
        List<OrderItem> orderItems = new ArrayList<>();

        for(OrderItemRequest itemRequest : request.items()){
            Product product = productsById.get(itemRequest.productId());

            if(product == null){
                throw new ProductNotFoundException();
            }

            OrderItem orderItem = OrderItem.create(
                    order,
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    new Quantity(itemRequest.quantity())
            );

            product.decreaseStockQuantity(orderItem.getQuantity());

            order.addItem(orderItem);
            orderItems.add(orderItem);
        }

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
    }

    public OrderResponse getOrder(Long userId, Long orderId){
        Order order = orderRepository.findByUserIdAndId(userId, orderId)
                .orElseThrow(OrderNotFoundException::new);

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(orderId);
        return OrderResponse.from(order, orderItems);
    }

    public Slice<OrderSummaryResponse> getOrders(Long userId, Pageable pageable){
        Slice<Order> orders = orderRepository.findAllByUserId(userId, pageable);

        return orders.map(OrderSummaryResponse::from);
    }

    @Transactional
    public void cancel(Long userId, Long orderId){
        Order order = orderRepository.findByUserIdAndId(userId, orderId)
                .orElseThrow(OrderNotFoundException::new);
        order.cancel();

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(orderId);
        List<Long> productIds = orderItems.stream().map(OrderItem::getProductId).toList();

        Map<Long, Product> productById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        product -> product
                ));

        for(OrderItem item : orderItems){
            Product product = productById.get(item.getProductId());
            product.increaseStockQuantity(item.getQuantity());
        }
    }
}
