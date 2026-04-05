-- ============================================================
-- V9 — Tags / Labels
-- Personal per-user tags that can be attached to expenses
-- via a many-to-many join table.
-- ============================================================

CREATE TABLE tags (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(7)   NOT NULL DEFAULT '#6366f1',
    user_id    BIGINT       NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tags_name_user UNIQUE (name, user_id)
);

CREATE TABLE expense_tags (
    expense_id BIGINT NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    tag_id     BIGINT NOT NULL REFERENCES tags(id)     ON DELETE CASCADE,
    PRIMARY KEY (expense_id, tag_id)
);

CREATE INDEX idx_tags_user_id             ON tags(user_id);
CREATE INDEX idx_expense_tags_expense_id  ON expense_tags(expense_id);
CREATE INDEX idx_expense_tags_tag_id      ON expense_tags(tag_id);
