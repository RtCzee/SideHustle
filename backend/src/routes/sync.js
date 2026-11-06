const express = require('express');
const { requireAuth } = require('../middleware/auth');
const { withTransaction } = require('../db/pool');
const { findClientById } = require('../db/clients');
const { findJobById } = require('../db/jobs');

const router = express.Router();
const handlers = new Map(
  ['me', 'clients', 'jobs', 'expenses', 'income', 'invoices']
    .map(kind => [kind, require('./' + kind).writes])
);
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const actions = ['create', 'update', 'remove', 'upsert', 'ensure'];

async function applyOperation(user, operation, transact = withTransaction) {
  const { operation_id: operationId, record_id: recordId, kind, payload } = operation || {};
  let action = operation?.action;
  const writes = handlers.get(kind);
  if (!writes || !actions.includes(action) ||
      typeof operationId !== 'string' || !uuid.test(operationId) || typeof recordId !== 'string' ||
      (kind === 'me' ? recordId !== user.uid : !uuid.test(recordId)) ||
      !payload || typeof payload !== 'object' || Array.isArray(payload)) {
    throw Object.assign(new Error('Invalid sync operation.'), { status: 400 });
  }

  return transact(async db => {
    await db.query('SELECT pg_advisory_xact_lock(hashtext($1), 1)', [user.uid]);
    const previous = await db.query(
      'SELECT response FROM sync_operations WHERE user_id = $1 AND operation_id = $2',
      [user.uid, operationId]
    );
    if (previous.rows.length) return previous.rows[0].response;

    // Version 1 of the app used upsert for both edits and new local records.
    if (action === 'upsert') {
      const lookup = kind === 'clients' ? findClientById : kind === 'jobs' ? findJobById : null;
      action = lookup && await lookup(user.uid, recordId) ? 'update' : 'create';
    }
    const handler = writes[action];
    if (!handler) throw Object.assign(new Error('Unsupported sync operation.'), { status: 400 });
    let status = 200;
    let body;
    const response = {
      status(code) { status = code; return this; },
      json(value) { body = value; },
    };
    await handler(
      { user, body: payload, recordId, params: { id: recordId, invoiceId: recordId } },
      response,
      error => { throw error; }
    );
    // A record already removed by another device satisfies an offline delete.
    if (action === 'remove' && status === 404) body = { deleted: true };
    else if (status >= 400) throw Object.assign(new Error(body.error), { status });
    await db.query(
      'INSERT INTO sync_operations (user_id, operation_id, response) VALUES ($1, $2, $3)',
      [user.uid, operationId, body]
    );
    return body;
  });
}

router.post('/', requireAuth, async (req, res, next) => {
  const { user_id: userId, operations } = req.body || {};
  if (userId != null && userId !== req.user.uid) return res.status(403).json({ error: 'The active account changed.' });
  if (!Array.isArray(operations) || !operations.length || operations.length > 500) {
    return res.status(400).json({ error: 'Provide between 1 and 500 operations.' });
  }
  const results = {};
  try {
    for (const operation of operations) {
      const result = await applyOperation(req.user, operation);
      results[operation.operation_id] = result;
    }
    res.json({ accepted_operation_ids: Object.keys(results), results });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
module.exports.applyOperation = applyOperation;
