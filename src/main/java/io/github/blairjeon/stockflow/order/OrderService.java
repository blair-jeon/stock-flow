package io.github.blairjeon.stockflow.order;

import io.github.blairjeon.stockflow.order.dto.OrderCreateRequest;
import io.github.blairjeon.stockflow.order.dto.OrderItemRequest;
import io.github.blairjeon.stockflow.order.dto.OrderResponse;
import io.github.blairjeon.stockflow.order.dto.OrderSummaryResponse;
import io.github.blairjeon.stockflow.order.exception.OrderNotFoundException;
import io.github.blairjeon.stockflow.order.repository.OrderItemRepository;
import io.github.blairjeon.stockflow.order.repository.OrderRepository;
import io.github.blairjeon.stockflow.product.Product;
import io.github.blairjeon.stockflow.product.ProductStatus;
import io.github.blairjeon.stockflow.product.repository.ProductRepository;
import io.github.blairjeon.stockflow.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void create(Long userId, OrderCreateRequest request){
        List<OrderItemRequest> sortedRequest = request.items().stream()
                .sorted()
                .toList();

        List<Long> productIds = sortedRequest.stream()
                .map(OrderItemRequest::productId)
                .distinct()
                .toList();

        if(sortedRequest.size()!=productIds.size()){
            throw new RuntimeException("오류오류오류/아직exception종류미정");
        }

        Map<Long, Product> productsById = productRepository.findAllByIdInAndProductStatus(productIds, ProductStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Order order = Order.create(userId);
        List<OrderItem> items = new ArrayList<>();

        for(OrderItemRequest itemRequest : sortedRequest){
            Product product = productsById.get(itemRequest.productId());
            Quantity quantity = new Quantity(itemRequest.quantity());

            if(product==null){
                throw new RuntimeException("오류오류오류/exception종류미정");
            }

            int updateRows = productRepository.decreaseStock(
                    product.getId(),
                    quantity.value(),
                    product.getPrice().amount()
            );

            if(updateRows!=1){
                throw new RuntimeException("미정미정미정");
            }

            OrderItem item = OrderItem.create(order, product.getId(), product.getName(), product.getPrice(), quantity);

            order.addItem(item);
            items.add(item);
        }

        orderItemRepository.saveAll(items);
        orderRepository.save(order);
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
        Order order = orderRepository.findWithLockByUserIdAndId(userId, orderId)
                .orElseThrow(OrderNotFoundException::new);

        order.cancel();

        List<OrderItem> items = orderItemRepository.findAllByOrderId(orderId).stream()
                .sorted(Comparator.comparing(OrderItem::getProductId))
                .toList();

        if(items.isEmpty()){
            throw new RuntimeException("아이템없음/오류");
        }

        for(OrderItem item:items){
            int updateRows = productRepository.increaseStock(item.getProductId(), item.getQuantity().value());
            if(updateRows!=1){
                throw new RuntimeException("dhfbdfdafsaf");
            }
        }
    }
}
