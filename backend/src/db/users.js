const { getPool } = require('./pool');

const PROFILE_COLUMNS =
  'user_id, email, full_name, phone_number, profile_picture_url, preferred_currency, preferred_language, created_at, updated_at';

async function findUserById(userId) {
  const result = await getPool().query(
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

async function updateUser(userId, fields) {
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

  if (sets.length === 0) {
    return findUserById(userId);
  }

  sets.push(`updated_at = NOW()`);
  values.push(userId);

  const result = await getPool().query(
    `UPDATE users SET ${sets.join(', ')} WHERE user_id = $${index} RETURNING ${PROFILE_COLUMNS}`,
    values
  );

  return result.rows[0] ?? null;
}

module.exports = { findUserById, createUser, updateUser };
