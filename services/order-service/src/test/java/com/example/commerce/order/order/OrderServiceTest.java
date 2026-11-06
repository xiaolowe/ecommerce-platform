package com.example.commerce.order.order;

import com.example.commerce.order.client.CatalogClient;
import com.example.commerce.order.client.InventoryClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.example.commerce.order.order.OrderDtos.CreateOrderRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @MockBean
    private CatalogClient catalogClient;

    @MockBean
    private InventoryClient inventoryClient;

    @Test
    void createsOrderAfterSuccessfulStockReservation() {
        when(catalogClient.getProduct("product-1", "token"))
                .thenReturn(new CatalogClient.ProductSnapshot(
                        "product-1", "SKU-1", "Test Product", 1999, "CNY", "ACTIVE"));
        when(inventoryClient.reserve(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new InventoryClient.ReservationResult(
                        "reservation-1", "order-1", "product-1", 2, "RESERVED"));

        var response = orderService.create(
                "customer-1", "token", new CreateOrderRequest("product-1", 2));

        assertThat(response.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(response.totalCents()).isEqualTo(3998);
        assertThat(response.items()).hasSize(1);
    }
}
