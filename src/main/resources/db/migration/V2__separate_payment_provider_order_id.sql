-- ============================================================================
-- Separate payment provider order ID from final transaction/payment ID.
-- ============================================================================

ALTER TABLE payments
    ADD COLUMN provider_order_id VARCHAR(100) NULL AFTER payment_status;

UPDATE payments
SET provider_order_id = transaction_id
WHERE provider_order_id IS NULL
  AND payment_status = 'CREATED'
  AND transaction_id IS NOT NULL;
