ALTER TABLE customer_orders ADD COLUMN payment_expires_at TIMESTAMP(6);
ALTER TABLE customer_orders ADD COLUMN cancelled_at TIMESTAMP(6);
ALTER TABLE customer_orders ADD COLUMN cancellation_reason VARCHAR(40);
ALTER TABLE customer_orders ADD COLUMN stock_released BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE customer_orders
   SET payment_expires_at = TIMESTAMPADD(SECOND, 900, created_at)
 WHERE payment_expires_at IS NULL;

CREATE INDEX idx_orders_status_payment_expiry ON customer_orders (status, payment_expires_at);
CREATE INDEX idx_orders_cancelled_release ON customer_orders (status, stock_released, cancelled_at);
