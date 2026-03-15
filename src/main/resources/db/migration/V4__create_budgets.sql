-- ============================================================
-- V4 — Budgets
-- category_id nullable: NULL = overall (not category-scoped) budget.
-- period: DAILY | WEEKLY | MONTHLY | YEARLY
-- ============================================================

CREATE TABLE budgets (
    id              BIGSERIAL        PRIMARY KEY,
    user_id         BIGINT           NOT NULL,
    category_id     BIGINT           REFERENCES categories(id),
    amount          DOUBLE PRECISION NOT NULL,
    period          VARCHAR(20)      NOT NULL,
    start_date      DATE             NOT NULL,
    end_date        DATE,
    alert_threshold DOUBLE PRECISION NOT NULL DEFAULT 0.80,
    is_active       BOOLEAN          NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_budgets_user_id   ON budgets(user_id);
CREATE INDEX idx_budgets_is_active ON budgets(is_active);
