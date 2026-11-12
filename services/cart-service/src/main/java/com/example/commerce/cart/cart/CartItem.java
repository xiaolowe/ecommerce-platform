package com.example.commerce.cart.cart;

import java.time.Instant;

public class CartItem {

    private String id;
    private String customerId;
    private String productId;
    private String sku;
    private String productName;
    private long unitPriceCents;
    private String currency;
    private long quantity;
    private Instant createdAt;
    private Instant updatedAt;

    public CartItem() {
    }

    public CartItem(String id, String customerId, String productId, String sku,
                    String productName, long unitPriceCents, String currency,
                    long quantity, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.unitPriceCents = unitPriceCents;
        this.currency = currency;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public long getUnitPriceCents() {
        return unitPriceCents;
    }

    public void setUnitPriceCents(long unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
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
}
