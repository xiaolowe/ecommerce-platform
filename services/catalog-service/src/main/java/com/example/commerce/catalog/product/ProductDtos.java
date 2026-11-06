package com.example.commerce.catalog.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record CreateProductRequest(
            @NotBlank @Size(max = 64) String sku,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @Positive long priceCents,
            @NotBlank @Size(min = 3, max = 3) String currency
    ) {
    }

    public record ProductResponse(
            String id,
            String sku,
            String name,
            String description,
            long priceCents,
            String currency,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
