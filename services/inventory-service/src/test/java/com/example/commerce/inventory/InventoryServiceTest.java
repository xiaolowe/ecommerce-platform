package com.example.commerce.inventory;

import com.example.commerce.inventory.reservation.StockReservationMapper;
import com.example.commerce.inventory.stock.StockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.example.commerce.inventory.InventoryDtos.CreateStockRequest;
import static com.example.commerce.inventory.InventoryDtos.ReleaseStockRequest;
import static com.example.commerce.inventory.InventoryDtos.ReserveStockRequest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private StockReservationMapper reservationMapper;

    @BeforeEach
    void clearInventory() {
        reservationMapper.deleteAll();
        stockMapper.deleteAll();
    }

    @Test
    void releasesReservationIdempotently() {
        String productId = "product-idempotent";
        inventoryService.createStock(new CreateStockRequest(productId, 10));
        inventoryService.reserve(new ReserveStockRequest("order-idempotent", productId, 3));

        var reserved = inventoryService.getStock(productId);
        assertThat(reserved.availableStock()).isEqualTo(7);
        assertThat(reserved.reservedStock()).isEqualTo(3);

        inventoryService.release(new ReleaseStockRequest("order-idempotent", productId));
        inventoryService.release(new ReleaseStockRequest("order-idempotent", productId));

        var released = inventoryService.getStock(productId);
        assertThat(released.availableStock()).isEqualTo(10);
        assertThat(released.reservedStock()).isZero();
    }
}
