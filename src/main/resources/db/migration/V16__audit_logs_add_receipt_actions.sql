-- V16: Add RECEIPT_UPLOADED and RECEIPT_DELETED to the audit_logs action check constraint.
-- The constraint must be dropped and recreated because PostgreSQL does not support ALTER CHECK.

ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;

ALTER TABLE audit_logs ADD CONSTRAINT audit_logs_action_check CHECK (action IN (
    -- Expense
    'EXPENSE_CREATED', 'EXPENSE_UPDATED', 'EXPENSE_DELETED', 'EXPENSE_AUTO_CREATED',
    -- Budget
    'BUDGET_CREATED', 'BUDGET_UPDATED', 'BUDGET_DELETED',
    -- Recurring Expense
    'RECURRING_EXPENSE_CREATED', 'RECURRING_EXPENSE_UPDATED', 'RECURRING_EXPENSE_DELETED', 'RECURRING_EXPENSE_PROCESSED',
    -- Category
    'CATEGORY_CREATED', 'CATEGORY_UPDATED', 'CATEGORY_DELETED',
    -- Auth
    'USER_REGISTERED', 'USER_LOGIN', 'ROLE_ASSIGNED',
    -- Notification
    'NOTIFICATION_READ', 'NOTIFICATION_DELETED',
    -- Reports
    'REPORT_EXPORTED',
    -- Tags
    'TAG_CREATED', 'TAG_UPDATED', 'TAG_DELETED',
    -- Currency
    'EXCHANGE_RATE_SYNCED', 'USER_CURRENCY_UPDATED',
    -- Receipts
    'RECEIPT_UPLOADED', 'RECEIPT_DELETED'
));
