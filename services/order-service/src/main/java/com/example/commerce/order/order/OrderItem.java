package com.example.commerce.order.order;

public class OrderItem {

    private Long id;

    private String productId;

    private String sku;

    private String productName;

    private long unitPriceCents;

    private long quantity;

    public OrderItem() {
    }

    public OrderItem(String productId, String sku, String productName, long unitPriceCents, long quantity) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.unitPriceCents = unitPriceCents;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }
}
