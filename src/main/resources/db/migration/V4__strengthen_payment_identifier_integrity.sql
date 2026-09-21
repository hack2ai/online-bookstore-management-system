-- ============================================================================
-- Prevent a provider order or final transaction ID from being reused.
-- Nullable unique columns still allow multiple pre-payment rows with NULL IDs.
-- ============================================================================

ALTER TABLE payments
    ADD CONSTRAINT uk_payments_provider_order_id UNIQUE (provider_order_id),
    ADD CONSTRAINT uk_payments_transaction_id UNIQUE (transaction_id);
