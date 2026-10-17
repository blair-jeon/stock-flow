package io.github.blairjeon.stockflow.order;

import io.github.blairjeon.stockflow.order.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public OrderCreateResponse create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OrderCreateRequest request
    ){
        return orderService.create(userId, request);
    }

    @PostMapping("/{orderId}")
    public OrderCancelResponse cancel(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId
    ){
        return orderService.cancel(userId, orderId);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId
    ){
        return orderService.getOrder(userId, orderId);
    }

    @GetMapping
    public List<OrderDetailResponse> getOrders(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(
                    sort = {"updatedAt", "id"},
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ){
        return orderService.getOrders(userId, pageable);
    }
}