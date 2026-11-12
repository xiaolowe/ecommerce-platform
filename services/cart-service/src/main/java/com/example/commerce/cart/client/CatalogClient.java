package com.example.commerce.cart.client;

import com.example.commerce.platform.common.error.BadRequestException;
import com.example.commerce.platform.common.error.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(RestClient.Builder builder,
                         @Value("${clients.catalog.base-url:http://localhost:8082}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ProductSnapshot getProduct(String productId, String accessToken) {
        try {
            ProductSnapshot product = restClient.get()
                    .uri("/api/v1/products/{id}", productId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(ProductSnapshot.class);
            if (product == null) {
                throw new NotFoundException("Product not found: " + productId);
            }
            if (!"ACTIVE".equals(product.status())) {
                throw new BadRequestException("Product is not available");
            }
            return product;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new NotFoundException("Product not found: " + productId);
            }
            throw new BadRequestException("Catalog service rejected the request");
        }
    }

    public record ProductSnapshot(
            String id,
            String sku,
            String name,
            long priceCents,
            String currency,
            String status
    ) {
    }
}
