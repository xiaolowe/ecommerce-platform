package com.example.commerce.payment.payment;

import com.example.commerce.payment.client.OrderClient;
import com.example.commerce.platform.common.error.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.example.commerce.payment.payment.PaymentDtos.CreatePaymentRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentMapper paymentMapper;

    @MockBean
    private OrderClient orderClient;

    @BeforeEach
    void clearPayments() {
        paymentMapper.deleteAll();
    }

    @Test
    void createsPaymentFromOrderAndCompletesMockPayment() {
        when(orderClient.getOrder("order-1", "access-token"))
                .thenReturn(new OrderClient.OrderSnapshot(
                        "order-1", "customer-1", "PENDING_PAYMENT", 3998, "CNY",
                        Instant.now().plusSeconds(900)));

        var created = paymentService.create(
                "customer-1", "access-token", new CreatePaymentRequest("order-1", "MOCK"));

        assertThat(created.status()).isEqualTo("REQUIRES_ACTION");
        assertThat(created.amountCents()).isEqualTo(3998);
        assertThat(created.currency()).isEqualTo("CNY");
        assertThat(created.confirmPath()).endsWith("/mock-pay");

        var paid = paymentService.mockPay("customer-1", created.paymentNo());

        assertThat(paid.status()).isEqualTo("SUCCEEDED");
        assertThat(paid.orderSynced()).isTrue();
        assertThat(paid.paidAt()).isNotNull();
        verify(orderClient).markOrderPaid("order-1", 3998, "CNY", created.paymentNo());

        var adminPage = paymentService.listForAdmin(
                PaymentStatus.SUCCEEDED, created.paymentNo(), 0, 20);
        assertThat(adminPage.content()).anyMatch(item -> item.paymentNo().equals(created.paymentNo()));
    }

    @Test
    void reusesExistingPaymentForTheSameOrder() {
        when(orderClient.getOrder("order-2", "access-token"))
                .thenReturn(new OrderClient.OrderSnapshot(
                        "order-2", "customer-1", "PENDING_PAYMENT", 9900, "CNY",
                        Instant.now().plusSeconds(900)));

        var first = paymentService.create(
                "customer-1", "access-token", new CreatePaymentRequest("order-2", "MOCK"));
        var second = paymentService.create(
                "customer-1", "access-token", new CreatePaymentRequest("order-2", "MOCK"));

        assertThat(second.paymentNo()).isEqualTo(first.paymentNo());
        assertThat(paymentMapper.countAdmin(null, null)).isEqualTo(1);
    }

    @Test
    void rejectsUnsupportedPaymentChannel() {
        assertThatThrownBy(() -> paymentService.create(
                "customer-1", "access-token", new CreatePaymentRequest("order-3", "ALIPAY")))
                .hasMessageContaining("Unsupported payment channel");
    }

    @Test
    void marksLatePaymentForManualReviewWhenOrderWasClosed() {
        when(orderClient.getOrder("order-4", "access-token"))
                .thenReturn(new OrderClient.OrderSnapshot(
                        "order-4", "customer-1", "PENDING_PAYMENT", 1200, "CNY",
                        Instant.now().plusSeconds(900)));
        var created = paymentService.create(
                "customer-1", "access-token", new CreatePaymentRequest("order-4", "MOCK"));
        doThrow(new ConflictException("Order cannot be marked paid"))
                .when(orderClient)
                .markOrderPaid("order-4", 1200, "CNY", created.paymentNo());

        var result = paymentService.mockPay("customer-1", created.paymentNo());

        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.orderSynced()).isFalse();
        assertThat(result.manualReviewRequired()).isTrue();
        assertThat(result.orderSyncAttempts()).isEqualTo(1);
        assertThat(result.lastOrderSyncError()).contains("cannot be marked paid");
    }
}
