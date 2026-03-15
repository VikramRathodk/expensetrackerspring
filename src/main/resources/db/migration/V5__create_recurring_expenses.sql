-- ============================================================
-- V5 — Recurring Expenses
-- frequency: DAILY | WEEKLY | MONTHLY | YEARLY
-- next_due_date: the scheduler fires when next_due_date <= today
-- end_date null = runs forever
-- ============================================================

CREATE TABLE recurring_expenses (
    id            BIGSERIAL        PRIMARY KEY,
    user_id       BIGINT           NOT NULL,
    title         VARCHAR(255)     NOT NULL,
    amount        DOUBLE PRECISION NOT NULL,
    category_id   BIGINT           NOT NULL REFERENCES categories(id),
    frequency     VARCHAR(20)      NOT NULL,
    next_due_date DATE             NOT NULL,
    end_date      DATE,
    is_active     BOOLEAN          NOT NULL DEFAULT TRUE,
    note          TEXT,
    deleted_at    TIMESTAMP,
    created_at    TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recurring_user_id       ON recurring_expenses(user_id);
CREATE INDEX idx_recurring_next_due_date ON recurring_expenses(next_due_date);
CREATE INDEX idx_recurring_is_active     ON recurring_expenses(is_active);
