-- Sample rows for local demo / issue #8 verification
-- Apply after schema: npm run db:seed

INSERT INTO users (
    user_id, email, full_name, phone_number, preferred_currency, preferred_language
) VALUES (
    'demo-firebase-uid-001',
    'demo@sidehustle.dev',
    'Demo User',
    '+27000000000',
    'ZAR',
    'en'
) ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_settings (user_id)
VALUES ('demo-firebase-uid-001')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO clients (client_id, user_id, name, email, phone_number, notes)
VALUES (
    'a1111111-1111-4111-8111-111111111111',
    'demo-firebase-uid-001',
    'Thabo Tutoring',
    'thabo@example.com',
    '+27123456789',
    'Weekly maths sessions'
) ON CONFLICT (client_id) DO NOTHING;

INSERT INTO jobs (job_id, user_id, client_id, title, status, agreed_amount, currency)
VALUES (
    'b2222222-2222-4222-8222-222222222222',
    'demo-firebase-uid-001',
    'a1111111-1111-4111-8111-111111111111',
    'Grade 10 maths tutoring',
    'In Progress',
    1500.00,
    'ZAR'
) ON CONFLICT (job_id) DO NOTHING;

INSERT INTO income_records (income_id, user_id, amount, currency, description, payment_method)
VALUES (
    'c3333333-3333-4333-8333-333333333333',
    'demo-firebase-uid-001',
    500.00,
    'ZAR',
    'Deposit from Thabo',
    'EFT'
) ON CONFLICT (income_id) DO NOTHING;

INSERT INTO expense_records (expense_id, user_id, amount, currency, category, description)
VALUES (
    'd4444444-4444-4444-8444-444444444444',
    'demo-firebase-uid-001',
    120.00,
    'ZAR',
    'Transport',
    'Uber to client meeting'
) ON CONFLICT (expense_id) DO NOTHING;
