package com.example.commerce.payment.provider;

import com.example.commerce.payment.payment.Payment;

public interface PaymentProvider {

    String channel();

    PaymentInitiation initiate(Payment payment);

    record PaymentInitiation(String confirmPath) {
    }
}
