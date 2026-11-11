package com.example.commerce.order.order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomerOrder {

    private String id;

    private String customerId;

    private OrderStatus status;

    private long totalCents;

    private String currency;

    private long version;

    private Instant createdAt;

    private Instant updatedAt;

    private String paymentNo;

    private Instant paidAt;

    private Instant paymentExpiresAt;

    private Instant cancelledAt;

    private String cancellationReason;

    private boolean stockReleased;

    private List<OrderItem> items = new ArrayList<>();

    public CustomerOrder() {
    }

    public CustomerOrder(String id, String customerId, long totalCents, String currency,
                         Instant createdAt, Instant paymentExpiresAt) {
        this.id = id;
        this.customerId = customerId;
        this.status = OrderStatus.PENDING_STOCK;
        this.totalCents = totalCents;
        this.currency = currency;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.paymentExpiresAt = paymentExpiresAt;
        this.stockReleased = false;
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public void confirmStock() {
        transitionTo(OrderStatus.PENDING_PAYMENT);
    }

    public boolean cancel(String reason, Instant now) {
        if (status == OrderStatus.CANCELLED) {
            return false;
        }
        if (status != OrderStatus.PENDING_PAYMENT && status != OrderStatus.PENDING_STOCK) {
            throw new IllegalStateException("Order cannot be cancelled from its current state");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = now;
        this.cancellationReason = reason;
        this.updatedAt = now;
        return true;
    }

    public void markStockReleased(Instant now) {
        this.stockReleased = true;
        this.updatedAt = now;
    }

    public void markPaid(String paymentNo, Instant now) {
        if (status == OrderStatus.PAID) {
            if (!paymentNo.equals(this.paymentNo)) {
                throw new IllegalStateException("Order is already paid by another payment");
            }
            return;
        }
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Order is not waiting for payment");
        }
        this.status = OrderStatus.PAID;
        this.paymentNo = paymentNo;
        this.paidAt = now;
        this.updatedAt = now;
    }

    private void transitionTo(OrderStatus target) {
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("A cancelled order cannot change state");
        }
        this.status = target;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public void setTotalCents(long totalCents) {
        this.totalCents = totalCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getPaymentExpiresAt() {
        return paymentExpiresAt;
    }

    public void setPaymentExpiresAt(Instant paymentExpiresAt) {
        this.paymentExpiresAt = paymentExpiresAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public boolean isStockReleased() {
        return stockReleased;
    }

    public void setStockReleased(boolean stockReleased) {
        this.stockReleased = stockReleased;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
