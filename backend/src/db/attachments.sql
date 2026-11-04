CREATE TABLE IF NOT EXISTS attachments (
    attachment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(128) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    job_id UUID REFERENCES jobs(job_id) ON DELETE CASCADE,
    expense_id UUID REFERENCES expense_records(expense_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    storage_path TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (num_nonnulls(job_id, expense_id) = 1)
);
CREATE INDEX IF NOT EXISTS idx_attachments_job ON attachments(job_id);
CREATE INDEX IF NOT EXISTS idx_attachments_expense ON attachments(expense_id);
