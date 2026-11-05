const { Pool } = require('pg');

const { AsyncLocalStorage } = require('node:async_hooks');

const transaction = new AsyncLocalStorage();
let pool;

/**
 * Shared Postgres pool. Requires DATABASE_URL in .env (issue #8).
 */
function getPool() {
  const active = transaction.getStore();
  if (active) return active;
  if (!process.env.DATABASE_URL) {
    throw new Error(
      'DATABASE_URL is not set. Add it to backend/.env — see backend/README.md'
    );
  }

  if (!pool) {
    pool = new Pool({ connectionString: process.env.DATABASE_URL, connectionTimeoutMillis: 5000 });
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

async function withTransaction(work, pool = getPool()) {
  const active = transaction.getStore();
  if (active) return work(active);
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await transaction.run(client, () => work(client));
    await client.query('COMMIT');
    return result;
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}

module.exports = { getPool, checkDatabaseConnection, withTransaction };
