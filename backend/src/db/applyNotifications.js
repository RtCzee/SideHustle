require('dotenv').config();
const fs = require('fs');
const path = require('path');
const { getPool } = require('./pool');

async function run() {
  const pool = getPool();
  try {
    await pool.query(fs.readFileSync(path.join(__dirname, 'notifications.sql'), 'utf8'));
    console.log('Notification migration applied.');
  } finally { await pool.end(); }
}
run().catch(error => { console.error(error.message); process.exitCode = 1; });
