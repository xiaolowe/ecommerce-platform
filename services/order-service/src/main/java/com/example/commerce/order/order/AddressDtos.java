package com.example.commerce.order.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AddressDtos {

    private AddressDtos() {
    }

    public record SaveAddressRequest(
            @NotBlank @Size(max = 80) String recipientName,
            @NotBlank @Size(max = 32) String recipientPhone,
            @NotBlank @Size(max = 120) String region,
            @NotBlank @Size(max = 300) String detail,
            boolean defaultAddress
    ) {
    }

    public record AddressResponse(
            String id,
            String recipientName,
            String recipientPhone,
            String region,
            String detail,
            boolean defaultAddress,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ShippingAddressResponse(
            String recipientName,
            String recipientPhone,
            String region,
            String detail
    ) {
    }
}
