package com.example.commerce.order.order;

import com.example.commerce.platform.common.web.PageResult;
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
    PageResult<OrderResponse> list(@RequestParam(required = false) OrderStatus status,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return orderService.listForAdmin(
                status, keyword, Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }
}
