package com.example.commerce.order.order;

import com.example.commerce.order.client.CatalogClient;
import com.example.commerce.order.client.InventoryClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;

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

    @Test
    void marksPendingOrderAsPaid() {
        when(catalogClient.getProduct("product-2", "token"))
                .thenReturn(new CatalogClient.ProductSnapshot(
                        "product-2", "SKU-2", "Paid Product", 4900, "CNY", "ACTIVE"));
        when(inventoryClient.reserve(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new InventoryClient.ReservationResult(
                        "reservation-2", "order-2", "product-2", 1, "RESERVED"));

        var order = orderService.create(
                "customer-1", "token", new CreateOrderRequest("product-2", 1));
        orderService.markPaid(order.id(), new OrderDtos.MarkOrderPaidRequest(
                4900, "CNY", "PAY-TEST-1"));

        var paid = orderService.get("customer-1", order.id());
        assertThat(paid.status()).isEqualTo("PAID");
        assertThat(paid.paymentNo()).isEqualTo("PAY-TEST-1");
        assertThat(paid.paidAt()).isNotNull();

        var adminPage = orderService.listForAdmin(
                OrderStatus.PAID, "customer-1", PageRequest.of(0, 20));
        assertThat(adminPage.getContent()).anyMatch(item -> item.id().equals(order.id()));

        var customerPage = orderService.listForCustomer(
                "customer-1", OrderStatus.PAID, PageRequest.of(0, 20));
        assertThat(customerPage.getContent()).anyMatch(item -> item.id().equals(order.id()));

        var otherCustomerPage = orderService.listForCustomer(
                "customer-2", null, PageRequest.of(0, 20));
        assertThat(otherCustomerPage.getContent()).isEmpty();
    }
}
