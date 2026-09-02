const { Pool } = require('pg');

let pool;

/**
 * Shared Postgres pool. Requires DATABASE_URL in .env (issue #8).
 */
function getPool() {
  if (!process.env.DATABASE_URL) {
    throw new Error(
      'DATABASE_URL is not set. Add it to backend/.env — see backend/README.md'
    );
  }

  if (!pool) {
    pool = new Pool({ connectionString: process.env.DATABASE_URL });
  }

  return pool;
}
// funcion to check the database connection
async function checkDatabaseConnection() {
  const client = await getPool().connect();
  try {
    await client.query('SELECT 1');
    return true;
  } finally {
    client.release();
  }
}

module.exports = { getPool, checkDatabaseConnection };
