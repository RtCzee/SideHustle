require('dotenv').config();
const fs = require('node:fs');
const path = require('node:path');
const { getPool } = require('./pool');

async function run() {
  const pool = getPool();
  try {
    await pool.query(fs.readFileSync(path.join(__dirname, 'sync.sql'), 'utf8'));
    console.log('Sync migration applied.');
  } finally {
    await pool.end();
  }
}
run().catch(error => { console.error(error.message); process.exitCode = 1; });
