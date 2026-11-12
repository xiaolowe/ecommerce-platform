package com.example.commerce.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class CartClient {

    private final RestClient restClient;
    private final String internalApiKey;

    public CartClient(RestClient.Builder builder,
                      @Value("${clients.cart.base-url:http://localhost:8087}") String baseUrl,
                      @Value("${internal.api-key:local-internal-api-key-change-me}") String internalApiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public void completeCheckout(String customerId, List<CheckedOutItem> items) {
        restClient.post()
                .uri("/internal/cart/checkout-complete")
                .header("X-Internal-Token", internalApiKey)
                .body(new CheckoutCartRequest(customerId, items))
                .retrieve()
                .toBodilessEntity();
    }

    public record CheckoutCartRequest(String customerId, List<CheckedOutItem> items) {
    }

    public record CheckedOutItem(String productId, long quantity) {
    }
}
