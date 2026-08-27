require('dotenv').config();

const fs = require('fs');
const path = require('path');
const { getPool } = require('./pool');

async function applySeed() {
  const seedPath = path.join(__dirname, 'seed.sql');
  const sql = fs.readFileSync(seedPath, 'utf8');
  const pool = getPool();

  console.log('Applying seed from seed.sql...');
  await pool.query(sql);
  console.log('Seed data inserted.');
  await pool.end();
}

applySeed().catch((err) => {
  console.error('Failed to apply seed:', err.message);
  process.exit(1);
});
