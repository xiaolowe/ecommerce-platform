package com.example.commerce.cart.cart;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddCartItemRequest(
            @NotBlank String productId,
            @Positive long quantity
    ) {
    }

    public record UpdateCartItemRequest(@Positive long quantity) {
    }

    public record CartItemResponse(
            String id,
            String productId,
            String sku,
            String productName,
            long unitPriceCents,
            String currency,
            long quantity,
            long subtotalCents,
            Instant updatedAt
    ) {
    }

    public record CartResponse(
            List<CartItemResponse> items,
            long totalQuantity,
            long subtotalCents,
            Instant updatedAt
    ) {
    }

    public record CheckedOutItem(
            @NotBlank String productId,
            @Positive long quantity
    ) {
    }

    public record CheckoutCartRequest(
            @NotBlank String customerId,
            List<CheckedOutItem> items
    ) {
    }
}
