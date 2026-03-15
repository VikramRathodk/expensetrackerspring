-- V12: Exchange rates table
-- Stores daily rates fetched from open.er-api.com.
-- All rates are relative to USD as the pivot base:
--   base_currency = 'USD', target_currency = 'INR', rate = 83.5
--   means: 1 USD = 83.5 INR
-- Cross-rate for any A→B pair:  rate(A→B) = rate(USD→B) / rate(USD→A)

CREATE TABLE IF NOT EXISTS exchange_rates (
    id              BIGSERIAL       PRIMARY KEY,
    base_currency   VARCHAR(3)      NOT NULL,
    target_currency VARCHAR(3)      NOT NULL,
    rate            DOUBLE PRECISION NOT NULL,
    fetched_at      TIMESTAMP        NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_exchange_rates_pair UNIQUE (base_currency, target_currency)
);

CREATE INDEX IF NOT EXISTS idx_exchange_rates_base   ON exchange_rates(base_currency);
CREATE INDEX IF NOT EXISTS idx_exchange_rates_target ON exchange_rates(target_currency);
CREATE INDEX IF NOT EXISTS idx_exchange_rates_pair   ON exchange_rates(base_currency, target_currency);
