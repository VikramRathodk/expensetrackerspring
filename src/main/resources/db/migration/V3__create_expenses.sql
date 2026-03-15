-- ============================================================
-- V3 — Expenses
-- ============================================================

CREATE TABLE expenses (
    id          BIGSERIAL        PRIMARY KEY,
    title       VARCHAR(255)     NOT NULL,
    amount      DOUBLE PRECISION NOT NULL,
    category_id BIGINT           NOT NULL REFERENCES categories(id),
    user_id     BIGINT           NOT NULL,
    note        TEXT,
    created_at  TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expenses_user_id     ON expenses(user_id);
CREATE INDEX idx_expenses_category_id ON expenses(category_id);
CREATE INDEX idx_expenses_created_at  ON expenses(created_at);
