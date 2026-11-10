package com.example.commerce.payment.provider;

import com.example.commerce.payment.payment.Payment;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String channel() {
        return "MOCK";
    }

    @Override
    public PaymentInitiation initiate(Payment payment) {
        return new PaymentInitiation("/api/v1/payments/" + payment.getPaymentNo() + "/mock-pay");
    }
}
