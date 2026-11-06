package com.example.commerce.inventory.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "sku_stocks")
public class Stock {

    @Id
    @Column(name = "product_id", length = 36)
    private String productId;

    @Column(name = "available_stock", nullable = false)
    private long availableStock;

    @Column(name = "reserved_stock", nullable = false)
    private long reservedStock;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Stock() {
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

    public long getAvailableStock() {
        return availableStock;
    }

    public long getReservedStock() {
        return reservedStock;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
