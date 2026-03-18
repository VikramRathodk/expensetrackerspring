-- V15: Replace s3_key with file_data — store receipt bytes directly in the table

ALTER TABLE receipts DROP COLUMN IF EXISTS s3_key;
ALTER TABLE receipts ADD COLUMN IF NOT EXISTS file_data BYTEA NOT NULL DEFAULT '';
ALTER TABLE receipts ALTER COLUMN file_data DROP DEFAULT;
