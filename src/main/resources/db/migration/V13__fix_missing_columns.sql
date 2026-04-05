-- V13: Idempotent safety migration — ensures columns from V10/V11/V12 actually exist.
-- Needed because earlier startups may have recorded V10-V12 as applied in flyway_schema_history
-- before the SQL had a chance to execute (app failed mid-startup due to unrelated errors).

-- expenses: currency + amount_in_base
ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS currency       VARCHAR(3)         NOT NULL DEFAULT 'INR',
    ADD COLUMN IF NOT EXISTS amount_in_base DOUBLE PRECISION   NOT NULL DEFAULT 0;

UPDATE expenses SET amount_in_base = amount WHERE amount_in_base = 0;

CREATE INDEX IF NOT EXISTS idx_expenses_currency ON expenses(currency);

-- users: base_currency
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS base_currency VARCHAR(3) NOT NULL DEFAULT 'INR';

-- exchange_rates table
CREATE TABLE IF NOT EXISTS exchange_rates (
    id              BIGSERIAL PRIMARY KEY,
    base_currency   VARCHAR(3)         NOT NULL,
    target_currency VARCHAR(3)         NOT NULL,
    rate            DOUBLE PRECISION   NOT NULL,
    fetched_at      TIMESTAMP          NOT NULL,
    CONSTRAINT uq_exchange_rate_pair UNIQUE (base_currency, target_currency)
);

CREATE INDEX IF NOT EXISTS idx_er_base      ON exchange_rates(base_currency);
CREATE INDEX IF NOT EXISTS idx_er_target    ON exchange_rates(target_currency);
CREATE INDEX IF NOT EXISTS idx_er_fetched   ON exchange_rates(fetched_at);
