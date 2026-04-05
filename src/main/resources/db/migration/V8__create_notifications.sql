-- ============================================================
-- V8 — Notifications
-- Stores in-app notifications for users.
-- Triggered by budget alerts, recurring expense processing,
-- and system events.
-- ============================================================

CREATE TABLE notifications (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(150) NOT NULL,
    message     TEXT         NOT NULL,
    type        VARCHAR(50)  NOT NULL,
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    entity_type VARCHAR(50),
    entity_id   BIGINT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id        ON notifications(user_id);
CREATE INDEX idx_notifications_user_unread    ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_created_at     ON notifications(created_at DESC);
