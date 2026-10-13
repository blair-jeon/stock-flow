package io.github.blairjeon.stockflow.order;

import io.github.blairjeon.stockflow.order.dto.OrderCreateRequest;
import io.github.blairjeon.stockflow.order.dto.OrderResponse;
import io.github.blairjeon.stockflow.order.dto.OrderSummaryResponse;
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
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OrderCreateRequest request
    ){
        orderService.create(userId, request);
    }

    @PostMapping("/{orderId}/cancel")
    public void cancel(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId
    ){
        orderService.cancel(userId, orderId);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId
    ){
        return orderService.getOrder(userId, orderId);
    }

    @GetMapping
    public Slice<OrderSummaryResponse> getOrders(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(
                    size = 20,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ){
        return orderService.getOrders(userId, pageable);
    }
}