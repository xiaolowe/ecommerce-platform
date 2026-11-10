package com.example.commerce.payment.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "payment_no", nullable = false, unique = true, length = 40)
    private String paymentNo;

    @Column(name = "order_id", nullable = false, unique = true, length = 36)
    private String orderId;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 24)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentStatus status;

    @Column(name = "provider_trade_no", length = 80)
    private String providerTradeNo;

    @Column(name = "order_synced", nullable = false)
    private boolean orderSynced;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Payment() {
    }

    public Payment(String id, String paymentNo, String orderId, String customerId, long amountCents,
                   String currency, String channel, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.paymentNo = paymentNo;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amountCents = amountCents;
        this.currency = currency;
        this.channel = channel;
        this.status = PaymentStatus.REQUIRES_ACTION;
        this.orderSynced = false;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void succeed(String providerTradeNo, Instant now) {
        if (status == PaymentStatus.SUCCEEDED) {
            return;
        }
        if (status == PaymentStatus.CLOSED || status == PaymentStatus.FAILED) {
            throw new IllegalStateException("Payment is no longer payable");
        }
        this.status = PaymentStatus.SUCCEEDED;
        this.providerTradeNo = providerTradeNo;
        this.paidAt = now;
        this.updatedAt = now;
    }

    public void close(Instant now) {
        if (status != PaymentStatus.REQUIRES_ACTION) {
            return;
        }
        status = PaymentStatus.CLOSED;
        updatedAt = now;
    }

    public void markOrderSynced() {
        orderSynced = true;
    }

    public String getId() {
        return id;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public String getChannel() {
        return channel;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getProviderTradeNo() {
        return providerTradeNo;
    }

    public boolean isOrderSynced() {
        return orderSynced;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
