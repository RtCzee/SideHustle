const express = require('express');
const { requireAuth } = require('../middleware/auth');
const { getPool } = require('../db/pool');

const router = express.Router();
const STATUSES = ['Draft', 'Sent', 'Partially Paid', 'Paid', 'Overdue'];

router.get('/clients', requireAuth, async (req, res, next) => {
  try {
    const { rows } = await getPool().query(
      'SELECT client_id, name FROM clients WHERE user_id = $1 ORDER BY name', [req.user.uid]);
    res.json(rows);
  } catch (error) { next(error); }
});

router.get('/jobs', requireAuth, async (req, res, next) => {
  try {
    const { client_id: clientId } = req.query;
    if (!clientId) return res.status(400).json({ error: 'client_id is required.' });
    const { rows } = await getPool().query(
      `SELECT job_id, client_id, title, agreed_amount::float8 AS agreed_amount, currency
       FROM jobs WHERE user_id = $1 AND client_id = $2 ORDER BY created_at DESC`, [req.user.uid, clientId]);
    res.json(rows);
  } catch (error) { next(error); }
});

router.get('/', requireAuth, async (req, res, next) => {
  try {
    const { rows } = await getPool().query(
      `SELECT i.invoice_id, i.invoice_number, i.total_amount::float8 AS total_amount, i.status,
              i.issue_date, i.due_date, c.name AS client_name, i.job_id
       FROM invoices i JOIN clients c ON c.client_id = i.client_id
       WHERE i.user_id = $1 ORDER BY i.created_at DESC`, [req.user.uid]);
    res.json(rows);
  } catch (error) { next(error); }
});

router.post('/', requireAuth, async (req, res, next) => {
  const client = await getPool().connect();
  try {
    const { client_id: clientId, job_id: jobId } = req.body || {};
    if (!clientId || !jobId) return res.status(400).json({ error: 'Select both a client and a job.' });
    await client.query('BEGIN');
    const jobResult = await client.query(
      `SELECT agreed_amount, currency FROM jobs WHERE job_id = $1 AND client_id = $2 AND user_id = $3`,
      [jobId, clientId, req.user.uid]);
    if (!jobResult.rows[0]) { await client.query('ROLLBACK'); return res.status(400).json({ error: 'The selected job does not belong to this client.' }); }
    await client.query('SELECT pg_advisory_xact_lock(hashtext($1))', [req.user.uid]);
    const year = new Date().getUTCFullYear();
    const countResult = await client.query(
      `SELECT COUNT(*)::int AS count FROM invoices WHERE user_id = $1 AND invoice_number LIKE $2`,
      [req.user.uid, `INV-${year}-%`]);
    const invoiceNumber = `INV-${year}-${String(countResult.rows[0].count + 1).padStart(3, '0')}`;
    const job = jobResult.rows[0];
    const { rows } = await client.query(
      `INSERT INTO invoices (user_id, client_id, job_id, invoice_number, total_amount, currency, status)
       VALUES ($1, $2, $3, $4, $5, $6, 'Draft')
       RETURNING invoice_id, invoice_number, total_amount::float8 AS total_amount, status, issue_date, due_date`,
      [req.user.uid, clientId, jobId, invoiceNumber, job.agreed_amount, job.currency]);
    await client.query('COMMIT');
    res.status(201).json(rows[0]);
  } catch (error) { await client.query('ROLLBACK'); next(error); } finally { client.release(); }
});

router.patch('/:invoiceId/status', requireAuth, async (req, res, next) => {
  try {
    const { status } = req.body || {};
    if (!STATUSES.includes(status)) return res.status(400).json({ error: 'Invalid invoice status.' });
    const { rows } = await getPool().query(
      `UPDATE invoices SET status = $1 WHERE invoice_id = $2 AND user_id = $3
       RETURNING invoice_id, invoice_number, total_amount::float8 AS total_amount, status`,
      [status, req.params.invoiceId, req.user.uid]);
    if (!rows[0]) return res.status(404).json({ error: 'Invoice not found.' });
    res.json(rows[0]);
  } catch (error) { next(error); }
});

module.exports = router;
