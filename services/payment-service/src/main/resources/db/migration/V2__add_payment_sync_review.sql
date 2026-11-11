ALTER TABLE payments ADD COLUMN order_sync_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN last_order_sync_error VARCHAR(500);
ALTER TABLE payments ADD COLUMN manual_review_required BOOLEAN NOT NULL DEFAULT FALSE;
