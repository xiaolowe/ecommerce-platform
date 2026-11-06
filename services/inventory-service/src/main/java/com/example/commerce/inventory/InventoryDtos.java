package com.example.commerce.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    public record CreateStockRequest(
            @NotBlank String productId,
            @PositiveOrZero long availableStock
    ) {
    }

    public record ReserveStockRequest(
            @NotBlank String orderId,
            @NotBlank String productId,
            @Positive long quantity
    ) {
    }

    public record ReleaseStockRequest(
            @NotBlank String orderId,
            @NotBlank String productId
    ) {
    }

    public record StockResponse(
            String productId,
            long availableStock,
            long reservedStock,
            Instant updatedAt
    ) {
    }

    public record ReservationResponse(
            String reservationId,
            String orderId,
            String productId,
            long quantity,
            String status,
            Instant expiresAt
    ) {
    }
}
