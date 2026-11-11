package com.example.commerce.inventory;

import com.example.commerce.platform.common.error.UnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static com.example.commerce.inventory.InventoryDtos.ReleaseStockRequest;
import static com.example.commerce.inventory.InventoryDtos.ReservationResponse;
import static com.example.commerce.inventory.InventoryDtos.ReserveStockRequest;

@RestController
@RequestMapping("/internal/inventory/reservations")
public class InternalInventoryController {

    private final InventoryService inventoryService;
    private final byte[] internalApiKey;

    public InternalInventoryController(InventoryService inventoryService,
                                        @Value("${internal.api-key:local-internal-api-key-change-me}")
                                        String internalApiKey) {
        this.inventoryService = inventoryService;
        this.internalApiKey = internalApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping
    ReservationResponse reserve(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                @Valid @RequestBody ReserveStockRequest request) {
        authorize(token);
        return inventoryService.reserve(request);
    }

    @PostMapping("/release")
    ReservationResponse release(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                @Valid @RequestBody ReleaseStockRequest request) {
        authorize(token);
        return inventoryService.release(request);
    }

    private void authorize(String token) {
        if (token == null
                || !MessageDigest.isEqual(internalApiKey, token.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedException("Invalid internal token");
        }
    }
}
