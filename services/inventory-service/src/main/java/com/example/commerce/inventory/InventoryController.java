package com.example.commerce.inventory;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.example.commerce.inventory.InventoryDtos.CreateStockRequest;
import static com.example.commerce.inventory.InventoryDtos.ReleaseStockRequest;
import static com.example.commerce.inventory.InventoryDtos.ReservationResponse;
import static com.example.commerce.inventory.InventoryDtos.ReserveStockRequest;
import static com.example.commerce.inventory.InventoryDtos.StockResponse;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/stocks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    StockResponse createStock(@Valid @RequestBody CreateStockRequest request) {
        return inventoryService.createStock(request);
    }

    @GetMapping("/stocks/{productId}")
    StockResponse getStock(@PathVariable String productId) {
        return inventoryService.getStock(productId);
    }

    @PostMapping("/reservations")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    ReservationResponse reserve(@Valid @RequestBody ReserveStockRequest request) {
        return inventoryService.reserve(request);
    }

    @PostMapping("/reservations/release")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    ReservationResponse release(@Valid @RequestBody ReleaseStockRequest request) {
        return inventoryService.release(request);
    }
}
