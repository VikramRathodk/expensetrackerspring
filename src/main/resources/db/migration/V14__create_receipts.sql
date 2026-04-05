-- V14: Receipt attachments — stores metadata for files uploaded to S3

CREATE TABLE IF NOT EXISTS receipts (
    id           BIGSERIAL PRIMARY KEY,
    expense_id   BIGINT        NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id      BIGINT        NOT NULL,
    file_name    VARCHAR(255)  NOT NULL,
    file_data    BYTEA         NOT NULL,
    file_size    BIGINT        NOT NULL,
    content_type VARCHAR(100)  NOT NULL,
    uploaded_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_receipts_expense_id ON receipts(expense_id);
CREATE INDEX idx_receipts_user_id    ON receipts(user_id);
