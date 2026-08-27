require('dotenv').config();

const fs = require('fs');
const path = require('path');
const { getPool } = require('./pool');

async function applySchema() {
  const schemaPath = path.join(__dirname, 'schema.sql');
  const sql = fs.readFileSync(schemaPath, 'utf8');
  const pool = getPool();

  console.log('Applying schema from schema.sql...');
  await pool.query(sql);
  console.log('Schema applied successfully.');
  await pool.end();
}

applySchema().catch((err) => {
  console.error('Failed to apply schema:', err.message);
  process.exit(1);
});
