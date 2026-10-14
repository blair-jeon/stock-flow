package io.github.blairjeon.stockflow.order;

import io.github.blairjeon.stockflow.ErrorCode;
import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.order.dto.OrderCreateRequest;
import io.github.blairjeon.stockflow.order.dto.OrderItemRequest;
import io.github.blairjeon.stockflow.order.dto.OrderResponse;
import io.github.blairjeon.stockflow.order.dto.OrderSummaryResponse;
import io.github.blairjeon.stockflow.order.repository.OrderItemRepository;
import io.github.blairjeon.stockflow.order.repository.OrderRepository;
import io.github.blairjeon.stockflow.product.Product;
import io.github.blairjeon.stockflow.product.ProductStatus;
import io.github.blairjeon.stockflow.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void create(Long userId, OrderCreateRequest request) {
        List<OrderItemRequest> sortedRequest = request.items().stream()
                .sorted(Comparator.comparing(OrderItemRequest::productId))
                .toList();

        List<Long> productIds = sortedRequest.stream()
                .map(OrderItemRequest::productId)
                .distinct()
                .toList();

        if (sortedRequest.size() != productIds.size()) {
            throw new BusinessException(ErrorCode.DUPLICATE_ORDER_ITEM);
        }

        Map<Long, Product> productsById =
                productRepository.findAllById(productIds).stream()
                        .collect(Collectors.toMap(
                                Product::getId,
                                Function.identity()
                        ));

        Order order = Order.create(userId);
        List<OrderItem> items = new ArrayList<>();

        for (OrderItemRequest itemRequest : sortedRequest) {
            Product product = productsById.get(itemRequest.productId());

            if (product == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }

            if (product.getProductStatus() != ProductStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOR_SALE);
            }

            Quantity quantity = new Quantity(itemRequest.quantity());

            if (product.getStockQuantity().value() < quantity.value()) {
                throw new BusinessException(ErrorCode.NOT_ENOUGH_STOCK);
            }

            int updatedRows = productRepository.decreaseStock(
                    product.getId(),
                    quantity.value(),
                    product.getPrice().amount()
            );

            if (updatedRows == 0) {
                throw new BusinessException(ErrorCode.ORDER_CONFLICT);
            }

            if (updatedRows != 1) {
                throw new IllegalStateException(
                        "Unexpected stock update count: " + updatedRows
                );
            }

            OrderItem item = OrderItem.create(
                    order,
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    quantity
            );

            order.addItem(item);
            items.add(item);
        }

        // 주문을 먼저 저장한 뒤, 그 주문을 참조하는 항목을 저장
        orderRepository.save(order);
        orderItemRepository.saveAll(items);
    }

    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByUserIdAndId(userId, orderId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.ORDER_NOT_FOUND)
                );

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(orderId);

        return OrderResponse.from(order, orderItems);
    }

    public Slice<OrderSummaryResponse> getOrders(Long userId, Pageable pageable){
        Slice<Order> orders = orderRepository.findAllByUserId(userId, pageable);

        return orders.map(OrderSummaryResponse::from);
    }

    @Transactional
    public void cancel(Long userId, Long orderId){
        Order order = orderRepository.findWithLockByUserIdAndId(userId, orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.cancel();

        List<OrderItem> items = orderItemRepository.findAllByOrderId(orderId).stream()
                .sorted(Comparator.comparing(OrderItem::getProductId))
                .toList();

        if(items.isEmpty()){
            throw new IllegalStateException(
                    "Order has no items. orderId=" + orderId
            );
        }

        for(OrderItem item:items){
            int updateRows = productRepository.increaseStock(item.getProductId(), item.getQuantity().value());

            if(updateRows!=1){
                throw new IllegalStateException(
                        "Stock restore failed. orderId=" + orderId
                                + ", productId=" + item.getProductId()
                                + ", updatedRows=" + updateRows
                );
            }
        }
    }
}
