package com.example.commerce.payment.payment;

import java.time.Instant;

public class Payment {

    private String id;

    private String paymentNo;

    private String orderId;

    private String customerId;

    private long amountCents;

    private String currency;

    private String channel;

    private PaymentStatus status;

    private String providerTradeNo;

    private boolean orderSynced;

    private int orderSyncAttempts;

    private String lastOrderSyncError;

    private boolean manualReviewRequired;

    private Instant expiresAt;

    private Instant paidAt;

    private Instant createdAt;

    private Instant updatedAt;

    private long version;

    public Payment() {
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
        lastOrderSyncError = null;
        manualReviewRequired = false;
    }

    public void markOrderSyncFailed(String error, boolean manualReviewRequired) {
        orderSyncAttempts++;
        lastOrderSyncError = truncate(error == null ? "Unknown order synchronization error" : error, 500);
        this.manualReviewRequired = manualReviewRequired;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getProviderTradeNo() {
        return providerTradeNo;
    }

    public void setProviderTradeNo(String providerTradeNo) {
        this.providerTradeNo = providerTradeNo;
    }

    public boolean isOrderSynced() {
        return orderSynced;
    }

    public void setOrderSynced(boolean orderSynced) {
        this.orderSynced = orderSynced;
    }

    public int getOrderSyncAttempts() {
        return orderSyncAttempts;
    }

    public void setOrderSyncAttempts(int orderSyncAttempts) {
        this.orderSyncAttempts = orderSyncAttempts;
    }

    public String getLastOrderSyncError() {
        return lastOrderSyncError;
    }

    public void setLastOrderSyncError(String lastOrderSyncError) {
        this.lastOrderSyncError = lastOrderSyncError;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public void setManualReviewRequired(boolean manualReviewRequired) {
        this.manualReviewRequired = manualReviewRequired;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
