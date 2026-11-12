CREATE TABLE customer_addresses (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    recipient_name VARCHAR(80) NOT NULL,
    recipient_phone VARCHAR(32) NOT NULL,
    region VARCHAR(120) NOT NULL,
    detail VARCHAR(300) NOT NULL,
    default_address BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_addresses_customer ON customer_addresses (customer_id, default_address, updated_at);

ALTER TABLE customer_orders ADD COLUMN shipping_address_id VARCHAR(36);
ALTER TABLE customer_orders ADD COLUMN recipient_name VARCHAR(80);
ALTER TABLE customer_orders ADD COLUMN recipient_phone VARCHAR(32);
ALTER TABLE customer_orders ADD COLUMN shipping_region VARCHAR(120);
ALTER TABLE customer_orders ADD COLUMN shipping_detail VARCHAR(300);
