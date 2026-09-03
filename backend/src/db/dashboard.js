const { getPool } = require('./pool');
const { findUserById } = require('./users');

async function getDashboardMetrics(userId) {
  const profile = await findUserById(userId);
  if (!profile) {
    return null;
  }

  const pool = getPool();

  const [incomeRow, expenseRow, outstandingRow, jobsRow] = await Promise.all([
    pool.query(
      `SELECT COALESCE(SUM(amount), 0)::numeric AS total
       FROM income_records WHERE user_id = $1`,
      [userId]
    ),
    pool.query(
      `SELECT COALESCE(SUM(amount), 0)::numeric AS total
       FROM expense_records WHERE user_id = $1`,
      [userId]
    ),
    pool.query(
      `SELECT COALESCE(SUM(total_amount - amount_paid), 0)::numeric AS total
       FROM invoices
       WHERE user_id = $1 AND status NOT IN ('Paid', 'Draft')`,
      [userId]
    ),
    pool.query(
      `SELECT COUNT(*)::int AS count
       FROM jobs
       WHERE user_id = $1
         AND status = 'Completed'
         AND completed_date >= date_trunc('month', CURRENT_DATE)::date`,
      [userId]
    ),
  ]);

  const totalIncome = Number(incomeRow.rows[0].total);
  const totalExpenses = Number(expenseRow.rows[0].total);
  const netProfit = totalIncome - totalExpenses;
  const outstandingPayments = Number(outstandingRow.rows[0].total);
  const completedJobsThisMonth = jobsRow.rows[0].count;

  // Placeholder until issue #19 — score formula will replace this
  const sideHustleScore = Math.max(0, Math.min(100, Math.round(netProfit / 50)));

  return {
    full_name: profile.full_name,
    preferred_currency: profile.preferred_currency,
    total_income: totalIncome,
    total_expenses: totalExpenses,
    net_profit: netProfit,
    outstanding_payments: outstandingPayments,
    completed_jobs_this_month: completedJobsThisMonth,
    side_hustle_score: sideHustleScore,
  };
}

module.exports = { getDashboardMetrics };
