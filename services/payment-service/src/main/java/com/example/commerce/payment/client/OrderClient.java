package com.example.commerce.payment.client;

import com.example.commerce.platform.common.error.BadRequestException;
import com.example.commerce.platform.common.error.ConflictException;
import com.example.commerce.platform.common.error.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OrderClient {

    private final RestClient restClient;
    private final String internalApiKey;

    public OrderClient(RestClient.Builder builder,
                       @Value("${clients.order.base-url:http://localhost:8084}") String baseUrl,
                       @Value("${internal.api-key:local-internal-api-key-change-me}") String internalApiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public OrderSnapshot getOrder(String orderId, String accessToken) {
        try {
            OrderSnapshot order = restClient.get()
                    .uri("/api/v1/orders/{id}", orderId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(OrderSnapshot.class);
            if (order == null) {
                throw new NotFoundException("Order not found: " + orderId);
            }
            return order;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new NotFoundException("Order not found: " + orderId);
            }
            throw new BadRequestException("Order service rejected the payment request");
        }
    }

    public void markOrderPaid(String orderId, long amountCents, String currency, String paymentNo) {
        try {
            restClient.post()
                    .uri("/internal/orders/{id}/paid", orderId)
                    .header("X-Internal-Token", internalApiKey)
                    .body(new MarkPaidRequest(amountCents, currency, paymentNo))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                throw new ConflictException("Order cannot be marked paid");
            }
            throw new BadRequestException("Order service could not confirm payment");
        }
    }

    public record OrderSnapshot(
            String id,
            String customerId,
            String status,
            long totalCents,
            String currency,
            java.time.Instant paymentExpiresAt
    ) {
    }

    private record MarkPaidRequest(long amountCents, String currency, String paymentNo) {
    }
}
