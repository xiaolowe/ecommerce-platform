CREATE TABLE payments (
    id VARCHAR(36) PRIMARY KEY,
    payment_no VARCHAR(40) NOT NULL,
    order_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    channel VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    provider_trade_no VARCHAR(80),
    order_synced BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP(6) NOT NULL,
    paid_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_payments_payment_no UNIQUE (payment_no),
    CONSTRAINT uk_payments_order_id UNIQUE (order_id)
);

CREATE INDEX idx_payments_status_sync ON payments (status, order_synced);
CREATE INDEX idx_payments_status_expiry ON payments (status, expires_at);
