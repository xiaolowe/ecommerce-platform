package com.example.commerce.catalog.product;

import java.time.Instant;

public class Product {

    private String id;

    private String sku;

    private String name;

    private String description;

    private long priceCents;

    private String currency;

    private String imageUrl;

    private ProductStatus status;

    private long version;

    private Instant createdAt;

    private Instant updatedAt;

    public Product() {
    }

    public Product(String id, String sku, String name, String description, long priceCents,
                   String currency, String imageUrl, ProductStatus status, Instant createdAt) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.priceCents = priceCents;
        this.currency = currency;
        this.imageUrl = imageUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(long priceCents) {
        this.priceCents = priceCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateImage(String imageUrl) {
        this.imageUrl = imageUrl;
        this.updatedAt = Instant.now();
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
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
