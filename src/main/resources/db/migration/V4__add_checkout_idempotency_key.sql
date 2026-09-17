-- ============================================================================
-- Allow clients to safely retry checkout requests without creating duplicates.
-- ============================================================================

ALTER TABLE orders
    ADD COLUMN idempotency_key VARCHAR(100) NULL,
    ADD CONSTRAINT uk_orders_user_idempotency_key UNIQUE (user_id, idempotency_key);
