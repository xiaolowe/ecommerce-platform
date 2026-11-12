CREATE TABLE shopping_cart_items (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    unit_price_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    quantity BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_cart_customer_product UNIQUE (customer_id, product_id),
    CONSTRAINT ck_cart_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_cart_customer_updated ON shopping_cart_items (customer_id, updated_at);
