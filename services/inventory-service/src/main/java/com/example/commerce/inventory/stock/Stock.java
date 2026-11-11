package com.example.commerce.inventory.stock;

import java.time.Instant;

public class Stock {

    private String productId;

    private long availableStock;

    private long reservedStock;

    private long version;

    private Instant updatedAt;

    public Stock() {
    }

    public Stock(String productId, long availableStock, Instant updatedAt) {
        this.productId = productId;
        this.availableStock = availableStock;
        this.reservedStock = 0;
        this.updatedAt = updatedAt;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public long getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(long availableStock) {
        this.availableStock = availableStock;
    }

    public long getReservedStock() {
        return reservedStock;
    }

    public void setReservedStock(long reservedStock) {
        this.reservedStock = reservedStock;
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
}
