CREATE TABLE sku_stocks (
    product_id VARCHAR(36) PRIMARY KEY,
    available_stock BIGINT NOT NULL,
    reserved_stock BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT ck_sku_stocks_available CHECK (available_stock >= 0),
    CONSTRAINT ck_sku_stocks_reserved CHECK (reserved_stock >= 0)
);

CREATE TABLE stock_reservations (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    quantity BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    released_at TIMESTAMP(6),
    CONSTRAINT uk_reservations_order_product UNIQUE (order_id, product_id)
);

CREATE INDEX idx_reservations_expires_at ON stock_reservations (status, expires_at);
