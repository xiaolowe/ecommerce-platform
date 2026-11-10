package com.example.commerce.payment.payment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.example.commerce.payment.payment.PaymentDtos.CreatePaymentRequest;
import static com.example.commerce.payment.payment.PaymentDtos.PaymentResponse;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PaymentResponse create(@AuthenticationPrincipal Jwt jwt,
                           @Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.create(jwt.getSubject(), jwt.getTokenValue(), request);
    }

    @GetMapping("/{paymentNo}")
    PaymentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String paymentNo) {
        return paymentService.get(jwt.getSubject(), paymentNo);
    }

    @PostMapping("/{paymentNo}/mock-pay")
    PaymentResponse mockPay(@AuthenticationPrincipal Jwt jwt, @PathVariable String paymentNo) {
        return paymentService.mockPay(jwt.getSubject(), paymentNo);
    }
}
