-- SideHustle PostgreSQL schema (issue #8)
-- Apply: npm run db:schema   OR   psql $DATABASE_URL -f src/db/schema.sql

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Firebase Auth uid is the primary key (not a UUID format)
CREATE TABLE users (
    user_id VARCHAR(128) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50),
    profile_picture_url TEXT,
    preferred_currency VARCHAR(10) NOT NULL DEFAULT 'ZAR',
    preferred_language VARCHAR(10) NOT NULL DEFAULT 'en',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_settings (
    user_id VARCHAR(128) PRIMARY KEY REFERENCES users (user_id) ON DELETE CASCADE,
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_days_before INTEGER NOT NULL DEFAULT 3 CHECK (reminder_days_before >= 0),
    default_currency VARCHAR(10) NOT NULL DEFAULT 'ZAR',
    language VARCHAR(10) NOT NULL DEFAULT 'en'
);

CREATE TABLE clients (
    client_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(128) NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone_number VARCHAR(50),
    address TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE jobs (
    job_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(128) NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients (client_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'Pending'
        CHECK (status IN ('Pending', 'In Progress', 'Completed', 'Cancelled')),
    start_date DATE,
    due_date DATE,
    completed_date DATE,
    agreed_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (agreed_amount >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'ZAR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE invoices (
    invoice_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(128) NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients (client_id) ON DELETE CASCADE,
    job_id UUID REFERENCES jobs (job_id) ON DELETE SET NULL,
    invoice_number VARCHAR(64) NOT NULL,
    issue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE,
    total_amount NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    amount_paid NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (amount_paid >= 0),
    status VARCHAR(32) NOT NULL DEFAULT 'Draft'
        CHECK (status IN ('Draft', 'Sent', 'Partially Paid', 'Paid', 'Overdue')),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, invoice_number)
);

CREATE TABLE income_records (
    income_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(128) NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    client_id UUID REFERENCES clients (client_id) ON DELETE SET NULL,
    job_id UUID REFERENCES jobs (job_id) ON DELETE SET NULL,
    invoice_id UUID REFERENCES invoices (invoice_id) ON DELETE SET NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'ZAR',
    date_received DATE NOT NULL DEFAULT CURRENT_DATE,
    description VARCHAR(255),
    payment_method VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE expense_records (
    expense_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(128) NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'ZAR',
    expense_date DATE NOT NULL DEFAULT CURRENT_DATE,
    category VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    receipt_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clients_user_id ON clients (user_id);
CREATE INDEX idx_jobs_user_id ON jobs (user_id);
CREATE INDEX idx_jobs_client_id ON jobs (client_id);
CREATE INDEX idx_invoices_user_id ON invoices (user_id);
CREATE INDEX idx_income_user_id ON income_records (user_id);
CREATE INDEX idx_expenses_user_id ON expense_records (user_id);
