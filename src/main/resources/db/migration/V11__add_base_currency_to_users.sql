-- V11: Add base_currency preference to users
-- base_currency = the currency all reports are aggregated in for this user (default: INR)

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS base_currency VARCHAR(3) NOT NULL DEFAULT 'INR';
