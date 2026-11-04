const { getPool } = require('./pool');

const PROFILE_COLUMNS =
  `user_id, email, full_name, phone_number, profile_picture_url, preferred_currency, preferred_language, created_at, updated_at,
   COALESCE((SELECT notifications_enabled FROM user_settings WHERE user_settings.user_id = users.user_id), TRUE) AS notifications_enabled`;

async function findUserById(userId, db = getPool()) {
  const result = await db.query(
    `SELECT ${PROFILE_COLUMNS} FROM users WHERE user_id = $1`,
    [userId]
  );
  return result.rows[0] ?? null;
}

async function createUser({
  userId,
  email,
  fullName,
  phoneNumber,
  preferredCurrency,
  preferredLanguage,
}) {
  const currency = preferredCurrency ?? 'ZAR';
  const language = preferredLanguage ?? 'en';
  const client = await getPool().connect();

  try {
    await client.query('BEGIN');
    const userResult = await client.query(
      `INSERT INTO users (user_id, email, full_name, phone_number, preferred_currency, preferred_language)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING ${PROFILE_COLUMNS}`,
      [userId, email, fullName, phoneNumber ?? null, currency, language]
    );
    await client.query(
      `INSERT INTO user_settings (user_id, default_currency, language)
       VALUES ($1, $2, $3)`,
      [userId, currency, language]
    );
    await client.query('COMMIT');
    return userResult.rows[0];
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}

async function updateUser(userId, fields, pool = getPool()) {
  const allowed = {
    full_name: fields.fullName,
    phone_number: fields.phoneNumber,
    profile_picture_url: fields.profilePictureUrl,
    preferred_currency: fields.preferredCurrency,
    preferred_language: fields.preferredLanguage,
  };

  const sets = [];
  const values = [];
  let index = 1;

  for (const [column, value] of Object.entries(allowed)) {
    if (value !== undefined) {
      sets.push(`${column} = $${index++}`);
      values.push(value);
    }
  }

  sets.push(`updated_at = NOW()`);
  values.push(userId);
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    await client.query(`UPDATE users SET ${sets.join(', ')} WHERE user_id = $${index}`, values);
    if (fields.notificationsEnabled !== undefined) {
      await client.query(
        `INSERT INTO user_settings(user_id, notifications_enabled) VALUES ($1, $2)
         ON CONFLICT (user_id) DO UPDATE SET notifications_enabled = EXCLUDED.notifications_enabled`,
        [userId, fields.notificationsEnabled]);
    }
    const profile = await findUserById(userId, client);
    await client.query('COMMIT');
    return profile;
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally { client.release(); }
}

module.exports = { findUserById, createUser, updateUser };
