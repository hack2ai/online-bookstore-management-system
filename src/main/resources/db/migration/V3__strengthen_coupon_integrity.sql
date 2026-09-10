-- ============================================================================
-- Strengthen coupon data integrity at the database boundary.
-- ============================================================================

ALTER TABLE coupons
    ADD CONSTRAINT chk_coupon_max_discount_nonneg
        CHECK (max_discount_amount IS NULL OR max_discount_amount >= 0),
    ADD CONSTRAINT chk_coupon_dates_ordered
        CHECK (expires_at > starts_at);
