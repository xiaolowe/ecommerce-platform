package com.example.commerce.order.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.example.commerce.order.order.OrderDtos.OrderResponse;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    Page<OrderResponse> list(@RequestParam(required = false) OrderStatus status,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return orderService.listForAdmin(status, keyword, pageable(page, size));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
