const { getPool } = require('./pool');

async function registerDevice(userId, { deviceId, token, enabled }, pool = getPool()) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    // A restored installation can receive an existing token. Keep one owner per token.
    await client.query('DELETE FROM push_devices WHERE fcm_token = $1 AND device_id <> $2', [token, deviceId]);
    await client.query(
      `INSERT INTO push_devices(device_id, user_id, fcm_token, enabled) VALUES ($1, $2, $3, $4)
       ON CONFLICT (device_id) DO UPDATE SET user_id = EXCLUDED.user_id,
         fcm_token = EXCLUDED.fcm_token, enabled = EXCLUDED.enabled, updated_at = NOW()`,
      [deviceId, userId, token, enabled]);
    await client.query('COMMIT');
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally { client.release(); }
}

module.exports = { registerDevice };
