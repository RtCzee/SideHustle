const express = require('express');
const { requireAuth } = require('../middleware/auth');
const { getPool, withTransaction } = require('../db/pool');
const { invoiceTotal } = require('../services/FinancialRules');

const router = express.Router();
const STATUSES = ['Draft', 'Sent', 'Partially Paid', 'Paid', 'Overdue'];

router.get('/clients', requireAuth, async (req, res, next) => {
  try {
    const { rows } = await getPool().query(
      'SELECT client_id, name FROM clients WHERE user_id = $1 ORDER BY name', [req.user.uid]
    );
    res.json(rows);
  } catch (error) { next(error); }
});

router.get('/jobs', requireAuth, async (req, res, next) => {
  try {
    const clientId = req.query.client_id;
    if (!clientId) return res.status(400).json({ error: 'client_id is required.' });
    const { rows } = await getPool().query(
      `SELECT job_id, client_id, title, agreed_amount::float8 AS agreed_amount, currency
       FROM jobs WHERE user_id = $1 AND client_id = $2 ORDER BY created_at DESC`,
      [req.user.uid, clientId]
    );
    res.json(rows);
  } catch (error) { next(error); }
});

router.get('/', requireAuth, async (req, res, next) => {
  try {
    const { rows } = await getPool().query(
      `SELECT i.invoice_id, i.invoice_number, i.total_amount::float8 AS total_amount, i.currency,
              i.status, i.client_id, i.job_id, i.amount_paid::float8 AS amount_paid,
              c.name AS client_name
       FROM invoices i JOIN clients c ON c.client_id = i.client_id
       WHERE i.user_id = $1 ORDER BY i.created_at DESC`,
      [req.user.uid]
    );
    res.json(rows);
  } catch (error) { next(error); }
});

async function create(req, res, next) {
  try {
    const { client_id: clientId, job_id: jobId } = req.body || {};
    if (!clientId || !jobId) return res.status(400).json({ error: 'Select both a client and a job.' });
    const invoice = await withTransaction(async client => {
      const jobResult = await client.query(
        `SELECT agreed_amount, currency FROM jobs
         WHERE job_id = $1 AND client_id = $2 AND user_id = $3`, [jobId, clientId, req.user.uid]
      );
      const job = jobResult.rows[0];
      if (!job) return null;
      await client.query('SELECT pg_advisory_xact_lock(hashtext($1))', [req.user.uid]);
      const year = new Date().getUTCFullYear();
      const sequence = await client.query(
        `SELECT COALESCE(MAX(split_part(invoice_number, '-', 3)::bigint), 0) AS last_number
         FROM invoices WHERE user_id = $1 AND invoice_number ~ $2`,
        [req.user.uid, `^INV-${year}-[0-9]+$`]
      );
      const number = `INV-${year}-${String(BigInt(sequence.rows[0].last_number) + 1n).padStart(3, '0')}`;
      const { rows } = await client.query(
        `INSERT INTO invoices (invoice_id, user_id, client_id, job_id, invoice_number, total_amount, currency, status)
         VALUES (COALESCE($1::uuid, gen_random_uuid()), $2, $3, $4, $5, $6, $7, 'Draft')
         RETURNING invoice_id, invoice_number, total_amount::float8 AS total_amount, currency, status, client_id, job_id,
                   amount_paid::float8 AS amount_paid, (SELECT name FROM clients WHERE clients.client_id = invoices.client_id) AS client_name`,
        [req.recordId, req.user.uid, clientId, jobId, number, invoiceTotal(job.agreed_amount), job.currency]
      );
      return rows[0];
    });
    if (!invoice) return res.status(400).json({ error: 'The selected job does not belong to this client.' });
    res.status(201).json(invoice);
  } catch (error) {
    next(error);
  }
}

router.post('/', requireAuth, create);

async function update(req, res, next) {
  try {
    const status = req.body?.status;
    if (!STATUSES.includes(status)) return res.status(400).json({ error: 'Invalid invoice status.' });
    const { rows } = await getPool().query(
      `UPDATE invoices SET status = $1 WHERE invoice_id = $2 AND user_id = $3
       RETURNING invoice_id, invoice_number, total_amount::float8 AS total_amount, currency, status, client_id, job_id,
                 amount_paid::float8 AS amount_paid, (SELECT name FROM clients WHERE clients.client_id = invoices.client_id) AS client_name`,
      [status, req.params.invoiceId, req.user.uid]
    );
    if (!rows[0]) return res.status(404).json({ error: 'Invoice not found.' });
    res.json(rows[0]);
  } catch (error) { next(error); }
}

router.patch('/:invoiceId/status', requireAuth, update);

module.exports = router;

module.exports.writes = { create, update };
