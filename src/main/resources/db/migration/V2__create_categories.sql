-- ============================================================
-- V2 — Categories
-- Unique constraint on (name, user_id): same name allowed
-- across different users; global categories have user_id = NULL.
-- ============================================================

CREATE TABLE categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    is_global   BOOLEAN      NOT NULL DEFAULT FALSE,
    user_id     BIGINT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (name, user_id)
);
