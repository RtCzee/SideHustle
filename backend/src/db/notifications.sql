CREATE TABLE IF NOT EXISTS push_devices (
    device_id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    fcm_token TEXT NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_push_devices_user ON push_devices(user_id);

CREATE TABLE IF NOT EXISTS job_reminder_deliveries (
    device_id UUID NOT NULL REFERENCES push_devices(device_id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(job_id) ON DELETE CASCADE,
    due_date DATE NOT NULL,
    claimed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    claim_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    sent_at TIMESTAMPTZ,
    PRIMARY KEY (device_id, job_id, due_date)
);
