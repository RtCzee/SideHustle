const express = require('express');
const { requireAuth } = require('../middleware/auth');
const { getPool } = require('../db/pool');

const { isIsoDate } = require('../services/FinancialRules');

const router = express.Router();
const PAYMENT_METHODS = ['Cash', 'EFT', 'Card'];

router.get('/options', requireAuth, async (req, res, next) => {
  try {
    const pool = getPool();
    const [clients, jobs, invoices] = await Promise.all([
      pool.query('SELECT client_id, name FROM clients WHERE user_id = $1 ORDER BY name', [req.user.uid]),
      pool.query('SELECT job_id, title FROM jobs WHERE user_id = $1 ORDER BY created_at DESC', [req.user.uid]),
      pool.query('SELECT invoice_id, invoice_number FROM invoices WHERE user_id = $1 ORDER BY created_at DESC', [req.user.uid]),
    ]);
    res.json({ clients: clients.rows, jobs: jobs.rows, invoices: invoices.rows });
  } catch (error) { next(error); }
});

router.get('/', requireAuth, async (req, res, next) => {
  try {
    const { rows } = await getPool().query(
      `SELECT income_id, amount::float8 AS amount, currency, date_received::text AS date_received,
              payment_method, description, client_id, job_id, invoice_id
       FROM income_records WHERE user_id = $1 ORDER BY income_records.date_received DESC, created_at DESC`, [req.user.uid]
    );
    res.json(rows);
  } catch (error) { next(error); }
});

async function create(req, res, next) {
  try {
    const { amount, date_received: dateReceived, payment_method: paymentMethod, description,
      client_id: clientId, job_id: jobId, invoice_id: invoiceId } = req.body || {};
    const parsedAmount = typeof amount === 'number' ? amount : Number(amount);
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0 || parsedAmount > 9999999999.99) return res.status(400).json({ error: 'Amount must be greater than 0.' });
    if (!isIsoDate(dateReceived)) return res.status(400).json({ error: 'A valid received date is required.' });
    if (!PAYMENT_METHODS.includes(paymentMethod)) return res.status(400).json({ error: 'Select Cash, EFT, or Card.' });
    if (description != null && (typeof description !== 'string' || description.length > 255)) return res.status(400).json({ error: 'Description must be 255 characters or fewer.' });

    const pool = getPool();
    const checks = await Promise.all([
      clientId ? pool.query('SELECT 1 FROM clients WHERE client_id = $1 AND user_id = $2', [clientId, req.user.uid]) : null,
      jobId ? pool.query('SELECT 1 FROM jobs WHERE job_id = $1 AND user_id = $2', [jobId, req.user.uid]) : null,
      invoiceId ? pool.query('SELECT 1 FROM invoices WHERE invoice_id = $1 AND user_id = $2', [invoiceId, req.user.uid]) : null,
    ]);
    if (checks.some((result) => result && !result.rows[0])) return res.status(400).json({ error: 'One selected linked record is not available.' });

    const { rows } = await pool.query(
      `INSERT INTO income_records (income_id, user_id, client_id, job_id, invoice_id, amount, date_received, payment_method, description)
       VALUES (COALESCE($1::uuid, gen_random_uuid()), $2, $3, $4, $5, $6, $7, $8, $9)
       RETURNING income_id, amount::float8 AS amount, currency, date_received::text AS date_received,
                 payment_method, description, client_id, job_id, invoice_id`,
      [req.recordId, req.user.uid, clientId || null, jobId || null, invoiceId || null, parsedAmount, dateReceived, paymentMethod, description?.trim() || null]
    );
    res.status(201).json(rows[0]);
  } catch (error) { next(error); }
}

router.post('/', requireAuth, create);

module.exports = router;

module.exports.writes = { create };
