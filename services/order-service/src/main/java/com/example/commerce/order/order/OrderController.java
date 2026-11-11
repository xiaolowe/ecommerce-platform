package com.example.commerce.order.order;

import jakarta.validation.Valid;
import com.example.commerce.platform.common.web.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.example.commerce.order.order.OrderDtos.CreateOrderRequest;
import static com.example.commerce.order.order.OrderDtos.OrderResponse;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse create(@AuthenticationPrincipal Jwt jwt,
                         @Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(jwt.getSubject(), jwt.getTokenValue(), request);
    }

    @GetMapping
    PageResult<OrderResponse> list(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam(required = false) OrderStatus status,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return orderService.listForCustomer(
                jwt.getSubject(), status, Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }

    @GetMapping("/{id}")
    OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return orderService.get(jwt.getSubject(), id);
    }

    @PostMapping("/{id}/cancel")
    OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return orderService.cancel(jwt.getSubject(), id);
    }
}
