const { getPool } = require('./pool');

const EXPENSE_CATEGORIES = ['Transport', 'Materials', 'Software', 'Other'];

async function listExpenses(userId) {
  const { rows } = await getPool().query(
    `SELECT expense_id, amount::float8 AS amount, currency, expense_date, category, description, created_at
     FROM expense_records
     WHERE user_id = $1
     ORDER BY expense_date DESC, created_at DESC`,
    [userId]
  );
  return rows;
}

async function createExpense(userId, { amount, expenseDate, category, description }) {
  const { rows } = await getPool().query(
    `INSERT INTO expense_records (user_id, amount, expense_date, category, description)
     VALUES ($1, $2, $3, $4, $5)
     RETURNING expense_id, amount::float8 AS amount, currency, expense_date, category, description, created_at`,
    [userId, amount, expenseDate, category, description || null]
  );
  return rows[0];
}

module.exports = { EXPENSE_CATEGORIES, listExpenses, createExpense };
