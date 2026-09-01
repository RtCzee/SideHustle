require('dotenv').config();

const { getPool } = require('./pool');

const TABLES = [
  'users',
  'user_settings',
  'clients',
  'jobs',
  'invoices',
  'income_records',
  'expense_records',
];

async function verifyDb() {
  const pool = getPool();
  const url = process.env.DATABASE_URL || '';
  const hostMatch = url.match(/@([^/]+)\//);
  const host = hostMatch ? hostMatch[1] : '(unknown)';

  console.log(`Connected to: ${host}`);
  console.log('Row counts:');

  for (const table of TABLES) {
    const result = await pool.query(`SELECT COUNT(*)::int AS count FROM ${table}`);
    console.log(`  ${table}: ${result.rows[0].count}`);
  }

  await pool.end();
}

verifyDb().catch((err) => {
  console.error('Failed to verify database:', err.message);
  process.exit(1);
});
