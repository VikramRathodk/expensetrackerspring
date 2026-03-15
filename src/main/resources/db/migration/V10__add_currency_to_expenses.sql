-- V10: Add currency support to expenses
-- currency     = ISO 4217 code for this expense (e.g. USD, EUR, INR)
-- amount_in_base = expense amount converted to the user's base currency at the time of creation

ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS currency      VARCHAR(3)     NOT NULL DEFAULT 'INR',
    ADD COLUMN IF NOT EXISTS amount_in_base DOUBLE PRECISION NOT NULL DEFAULT 0;

-- Backfill: for existing rows amount_in_base equals amount (single-currency assumption)
UPDATE expenses SET amount_in_base = amount WHERE amount_in_base = 0;

CREATE INDEX IF NOT EXISTS idx_expenses_currency ON expenses(currency);
