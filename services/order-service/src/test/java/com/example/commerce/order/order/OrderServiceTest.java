package com.example.commerce.order.order;

import com.example.commerce.order.client.CatalogClient;
import com.example.commerce.order.client.InventoryClient;
import com.example.commerce.platform.common.error.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;

import static com.example.commerce.order.order.OrderDtos.CreateOrderRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerOrderMapper orderMapper;

    @MockBean
    private CatalogClient catalogClient;

    @MockBean
    private InventoryClient inventoryClient;

    @Test
    void createsOrderAfterSuccessfulStockReservation() {
        when(catalogClient.getProduct("product-1", "token"))
                .thenReturn(new CatalogClient.ProductSnapshot(
                        "product-1", "SKU-1", "Test Product", 1999, "CNY", "ACTIVE"));
        when(inventoryClient.reserve(anyString(), anyString(), anyLong()))
                .thenReturn(new InventoryClient.ReservationResult(
                        "reservation-1", "order-1", "product-1", 2, "RESERVED"));

        var response = orderService.create(
                "customer-1", "token", new CreateOrderRequest("product-1", 2));

        assertThat(response.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(response.totalCents()).isEqualTo(3998);
        assertThat(response.items()).hasSize(1);
        assertThat(response.paymentExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void marksPendingOrderAsPaid() {
        when(catalogClient.getProduct("product-2", "token"))
                .thenReturn(new CatalogClient.ProductSnapshot(
                        "product-2", "SKU-2", "Paid Product", 4900, "CNY", "ACTIVE"));
        when(inventoryClient.reserve(anyString(), anyString(), anyLong()))
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
                OrderStatus.PAID, "customer-1", 0, 20);
        assertThat(adminPage.content()).anyMatch(item -> item.id().equals(order.id()));

        var customerPage = orderService.listForCustomer(
                "customer-1", OrderStatus.PAID, 0, 20);
        assertThat(customerPage.content()).anyMatch(item -> item.id().equals(order.id()));

        var otherCustomerPage = orderService.listForCustomer(
                "customer-2", null, 0, 20);
        assertThat(otherCustomerPage.content()).isEmpty();
    }

    @Test
    void cancelsOrderAndReleasesInventoryOnlyOnce() {
        when(catalogClient.getProduct("product-3", "token"))
                .thenReturn(new CatalogClient.ProductSnapshot(
                        "product-3", "SKU-3", "Cancel Product", 2200, "CNY", "ACTIVE"));
        when(inventoryClient.reserve(anyString(), anyString(), anyLong()))
                .thenReturn(new InventoryClient.ReservationResult(
                        "reservation-3", "order-3", "product-3", 1, "RESERVED"));

        var order = orderService.create(
                "customer-1", "token", new CreateOrderRequest("product-3", 1));
        var cancelled = orderService.cancel("customer-1", order.id());

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.cancellationReason()).isEqualTo("CUSTOMER_REQUEST");
        assertThat(cancelled.stockReleased()).isTrue();
        verify(inventoryClient).release(order.id(), "product-3");

        orderService.cancel("customer-1", order.id());
        verify(inventoryClient, times(1)).release(order.id(), "product-3");
    }

    @Test
    void expiresPastDueOrderAndReleasesInventory() {
        Instant now = Instant.now();
        CustomerOrder order = new CustomerOrder(
                "expired-order", "customer-expired", 1000, "CNY", now.minusSeconds(60), now.minusSeconds(1));
        order.addItem(new OrderItem("expired-product", "SKU-EXP", "Expired Product", 1000, 1));
        order.confirmStock();
        orderMapper.insert(order);
        order.getItems().forEach(item -> orderMapper.insertItem(order.getId(), item));

        orderService.cancelExpiredOrder(order.getId());

        var cancelled = orderService.get("customer-expired", order.getId());
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.cancellationReason()).isEqualTo("PAYMENT_TIMEOUT");
        assertThat(cancelled.stockReleased()).isTrue();
        verify(inventoryClient).release(order.getId(), "expired-product");
    }

    @Test
    void rejectsPaymentForCancelledOrder() {
        Instant now = Instant.now();
        CustomerOrder order = new CustomerOrder(
                "closed-order", "customer-closed", 1000, "CNY", now, now.plusSeconds(60));
        order.addItem(new OrderItem("closed-product", "SKU-CLOSED", "Closed Product", 1000, 1));
        order.confirmStock();
        order.cancel("CUSTOMER_REQUEST", now);
        orderMapper.insert(order);
        order.getItems().forEach(item -> orderMapper.insertItem(order.getId(), item));

        assertThatThrownBy(() -> orderService.markPaid(
                order.getId(), new OrderDtos.MarkOrderPaidRequest(1000, "CNY", "PAY-CLOSED")))
                .isInstanceOf(ConflictException.class);
    }
}
