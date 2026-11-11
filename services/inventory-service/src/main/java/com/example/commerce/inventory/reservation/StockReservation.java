package com.example.commerce.inventory.reservation;

import java.time.Instant;

public class StockReservation {

    private String id;

    private String orderId;

    private String productId;

    private long quantity;

    private ReservationStatus status;

    private Instant expiresAt;

    private Instant createdAt;

    private Instant releasedAt;

    public StockReservation() {
    }

    public StockReservation(String id, String orderId, String productId, long quantity,
                            Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.status = ReservationStatus.RESERVED;
    }

    public void release(Instant now) {
        this.status = ReservationStatus.RELEASED;
        this.releasedAt = now;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
        this.status = ReservationStatus.RELEASED;
    }
}
