package com.example.commerce.order.client;

import com.example.commerce.platform.common.error.BadRequestException;
import com.example.commerce.platform.common.error.ConflictException;
import com.example.commerce.platform.common.error.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder builder,
                           @Value("${clients.inventory.base-url:http://localhost:8083}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ReservationResult reserve(String orderId, String productId, long quantity, String accessToken) {
        try {
            return restClient.post()
                    .uri("/api/v1/inventory/reservations")
                    .header("Authorization", "Bearer " + accessToken)
                    .body(new ReserveRequest(orderId, productId, quantity))
                    .retrieve()
                    .body(ReservationResult.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new NotFoundException("Inventory not found for product: " + productId);
            }
            if (exception.getStatusCode().value() == 409) {
                throw new ConflictException("Insufficient inventory for product: " + productId);
            }
            throw new BadRequestException("Inventory service rejected the reservation");
        }
    }

    public void release(String orderId, String productId, String accessToken) {
        restClient.post()
                .uri("/api/v1/inventory/reservations/release")
                .header("Authorization", "Bearer " + accessToken)
                .body(new ReleaseRequest(orderId, productId))
                .retrieve()
                .toBodilessEntity();
    }

    private record ReserveRequest(String orderId, String productId, long quantity) {
    }

    private record ReleaseRequest(String orderId, String productId) {
    }

    public record ReservationResult(
            String reservationId,
            String orderId,
            String productId,
            long quantity,
            String status
    ) {
    }
}
