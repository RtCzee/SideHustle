const { getPool } = require('./pool');

// Evaluate dates in the app's default time zone, independently of the database host.
const ELIGIBLE = `
  FROM jobs j
  JOIN user_settings s ON s.user_id = j.user_id AND s.notifications_enabled = TRUE
  JOIN push_devices d ON d.user_id = j.user_id AND d.enabled = TRUE
  WHERE j.status IN ('Pending', 'In Progress')
    AND j.due_date >= (CURRENT_TIMESTAMP AT TIME ZONE $1)::date
    AND j.due_date <= (CURRENT_TIMESTAMP AT TIME ZONE $1)::date + s.reminder_days_before`;

function createReminderStore(pool = getPool(), timeZone = process.env.REMINDER_TIME_ZONE || 'Africa/Johannesburg') {
  return {
    async candidates() {
      const { rows } = await pool.query(
        `SELECT j.job_id, j.due_date::text, d.device_id, d.fcm_token ${ELIGIBLE}
         AND NOT EXISTS (
           SELECT 1 FROM job_reminder_deliveries r
           WHERE r.job_id = j.job_id AND r.device_id = d.device_id AND r.due_date = j.due_date
             AND (r.sent_at IS NOT NULL OR r.claimed_at > NOW() - INTERVAL '15 minutes'))
         ORDER BY j.due_date, j.job_id, d.device_id LIMIT 500`, [timeZone]);
      return rows;
    },
    async claim(candidate) {
      const { rows } = await pool.query(
        `INSERT INTO job_reminder_deliveries(device_id, job_id, due_date)
         SELECT d.device_id, j.job_id, j.due_date ${ELIGIBLE}
           AND j.job_id = $2 AND j.due_date = $3 AND d.device_id = $4 AND d.fcm_token = $5
         ON CONFLICT (device_id, job_id, due_date) DO UPDATE
           SET claimed_at = NOW(), claim_id = gen_random_uuid()
           WHERE job_reminder_deliveries.sent_at IS NULL
             AND job_reminder_deliveries.claimed_at < NOW() - INTERVAL '15 minutes'
         RETURNING claim_id`,
        [timeZone, candidate.job_id, candidate.due_date, candidate.device_id, candidate.fcm_token]);
      if (!rows.length) return null;
      // Resolve the current job data after claiming. Ownership/preferences are checked again.
      const result = await pool.query(
        `SELECT j.job_id, j.user_id, j.title, j.due_date::text, d.device_id, d.fcm_token ${ELIGIBLE}
           AND j.job_id = $2 AND j.due_date = $3 AND d.device_id = $4 AND d.fcm_token = $5`,
        [timeZone, candidate.job_id, candidate.due_date, candidate.device_id, candidate.fcm_token]);
      if (!result.rows.length) {
        await pool.query('DELETE FROM job_reminder_deliveries WHERE claim_id = $1', [rows[0].claim_id]);
        return null;
      }
      return { ...result.rows[0], claim_id: rows[0].claim_id };
    },
    async sent(reminder) {
      await pool.query('UPDATE job_reminder_deliveries SET sent_at = NOW() WHERE claim_id = $1', [reminder.claim_id]);
    },
    async release(reminder) {
      await pool.query('DELETE FROM job_reminder_deliveries WHERE claim_id = $1 AND sent_at IS NULL', [reminder.claim_id]);
    },
    async removeInvalidDevice(reminder) {
      await pool.query('DELETE FROM push_devices WHERE device_id = $1 AND fcm_token = $2',
        [reminder.device_id, reminder.fcm_token]);
    },
  };
}
module.exports = { createReminderStore };
