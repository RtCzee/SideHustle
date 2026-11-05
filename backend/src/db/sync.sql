CREATE TABLE IF NOT EXISTS sync_operations (
    user_id VARCHAR(128) NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    operation_id UUID NOT NULL,
    response JSONB NOT NULL,
    PRIMARY KEY (user_id, operation_id)
);

ALTER TABLE invoices ADD COLUMN IF NOT EXISTS currency VARCHAR(10);
UPDATE invoices AS invoice SET currency = job.currency
FROM jobs AS job WHERE invoice.job_id = job.job_id AND invoice.currency IS NULL;
UPDATE invoices SET currency = 'ZAR' WHERE currency IS NULL;
ALTER TABLE invoices ALTER COLUMN currency SET DEFAULT 'ZAR';
ALTER TABLE invoices ALTER COLUMN currency SET NOT NULL;
