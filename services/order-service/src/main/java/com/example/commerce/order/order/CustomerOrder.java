package com.example.commerce.order.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OrderStatus status;

    @Column(name = "total_cents", nullable = false)
    private long totalCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "payment_no", length = 40)
    private String paymentNo;

    @Column(name = "paid_at")
    private Instant paidAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderItem> items = new ArrayList<>();

    protected CustomerOrder() {
    }

    public CustomerOrder(String id, String customerId, long totalCents, String currency, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.status = OrderStatus.PENDING_STOCK;
        this.totalCents = totalCents;
        this.currency = currency;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public void confirmStock() {
        transitionTo(OrderStatus.PENDING_PAYMENT);
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
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

    public String getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
