package com.example.commerce.payment.payment;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record CreatePaymentRequest(
            @NotBlank String orderId,
            @NotBlank String channel
    ) {
    }

    public record PaymentResponse(
            String id,
            String paymentNo,
            String orderId,
            long amountCents,
            String currency,
            String channel,
            String status,
            String providerTradeNo,
            boolean orderSynced,
            int orderSyncAttempts,
            String lastOrderSyncError,
            boolean manualReviewRequired,
            String confirmPath,
            Instant expiresAt,
            Instant paidAt,
            Instant createdAt
    ) {
    }

}
