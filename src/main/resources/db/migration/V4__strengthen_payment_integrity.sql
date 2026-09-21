-- ============================================================================
-- Strengthen payment identifier integrity at the database boundary.
-- Provider order IDs and final transaction IDs must be unique when present.
-- ============================================================================

ALTER TABLE payments
    ADD CONSTRAINT uk_payments_provider_order_id UNIQUE (provider_order_id),
    ADD CONSTRAINT uk_payments_transaction_id UNIQUE (transaction_id);
