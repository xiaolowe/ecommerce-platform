package com.example.commerce.payment.payment;

import com.example.commerce.payment.client.OrderClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.example.commerce.payment.payment.PaymentDtos.CreatePaymentRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private OrderClient orderClient;

    @BeforeEach
    void clearPayments() {
        paymentRepository.deleteAll();
    }

    @Test
    void createsPaymentFromOrderAndCompletesMockPayment() {
        when(orderClient.getOrder("order-1", "access-token"))
                .thenReturn(new OrderClient.OrderSnapshot(
                        "order-1", "customer-1", "PENDING_PAYMENT", 3998, "CNY"));

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
                PaymentStatus.SUCCEEDED, created.paymentNo(), PageRequest.of(0, 20));
        assertThat(adminPage.getContent()).anyMatch(item -> item.paymentNo().equals(created.paymentNo()));
    }

    @Test
    void reusesExistingPaymentForTheSameOrder() {
        when(orderClient.getOrder("order-2", "access-token"))
                .thenReturn(new OrderClient.OrderSnapshot(
                        "order-2", "customer-1", "PENDING_PAYMENT", 9900, "CNY"));

        var first = paymentService.create(
                "customer-1", "access-token", new CreatePaymentRequest("order-2", "MOCK"));
        var second = paymentService.create(
                "customer-1", "access-token", new CreatePaymentRequest("order-2", "MOCK"));

        assertThat(second.paymentNo()).isEqualTo(first.paymentNo());
        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsUnsupportedPaymentChannel() {
        assertThatThrownBy(() -> paymentService.create(
                "customer-1", "access-token", new CreatePaymentRequest("order-3", "ALIPAY")))
                .hasMessageContaining("Unsupported payment channel");
    }
}
