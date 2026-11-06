package com.example.commerce.order.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record CreateOrderRequest(
            @NotBlank String productId,
            @Positive long quantity
    ) {
    }

    public record OrderItemResponse(
            String productId,
            String sku,
            String productName,
            long unitPriceCents,
            long quantity
    ) {
    }

    public record OrderResponse(
            String id,
            String customerId,
            String status,
            long totalCents,
            String currency,
            List<OrderItemResponse> items,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
