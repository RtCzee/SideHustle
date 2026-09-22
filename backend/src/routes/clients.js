const express = require('express');
const { requireAuth } = require('../middleware/auth');
const {
  listClients,
  findClientById,
  createClient,
  updateClient,
  deleteClient,
} = require('../db/clients');

const router = express.Router();

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function normalizeName(value) {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

/** Returns the trimmed email if valid, null if omitted/blank, or undefined if invalid. */
function normalizeEmail(value) {
  if (value === undefined || value === null) return null;
  if (typeof value !== 'string') return undefined;
  const trimmed = value.trim();
  if (trimmed.length === 0) return null;
  return EMAIL_PATTERN.test(trimmed) ? trimmed : undefined;
}

function clientPayload(row) {
  return {
    client_id: row.client_id,
    user_id: row.user_id,
    name: row.name,
    email: row.email,
    phone_number: row.phone_number,
    address: row.address,
    notes: row.notes,
    created_at: row.created_at,
    updated_at: row.updated_at,
  };
}

// A malformed UUID in the :id param makes Postgres throw invalid_text_representation (22P02).
// Treat that the same as "not found" rather than a 500.
function isInvalidIdError(error) {
  return error && error.code === '22P02';
}

/** GET /clients — list, alphabetical by name. */
router.get('/', requireAuth, async (req, res, next) => {
  try {
    const rows = await listClients(req.user.uid);
    res.json(rows.map(clientPayload));
  } catch (error) {
    next(error);
  }
});

/** POST /clients — create. */
router.post('/', requireAuth, async (req, res, next) => {
  try {
    const name = normalizeName(req.body.name);
    if (!name) {
      return res.status(400).json({ error: 'name is required.' });
    }

    const email = normalizeEmail(req.body.email);
    if (email === undefined) {
      return res.status(400).json({ error: 'email is not a valid email address.' });
    }

    const client = await createClient(req.user.uid, {
      name,
      email,
      phoneNumber: req.body.phone_number ?? req.body.phoneNumber ?? null,
      address: req.body.address ?? null,
      notes: req.body.notes ?? null,
    });

    res.status(201).json(clientPayload(client));
  } catch (error) {
    next(error);
  }
});

/** GET /clients/:id */
router.get('/:id', requireAuth, async (req, res, next) => {
  try {
    const client = await findClientById(req.user.uid, req.params.id);
    if (!client) {
      return res.status(404).json({ error: 'Client not found.' });
    }
    res.json(clientPayload(client));
  } catch (error) {
    if (isInvalidIdError(error)) {
      return res.status(404).json({ error: 'Client not found.' });
    }
    next(error);
  }
});

/** PUT /clients/:id — partial update; only provided fields change. */
router.put('/:id', requireAuth, async (req, res, next) => {
  try {
    const hasName = req.body.name !== undefined;
    let name;
    if (hasName) {
      name = normalizeName(req.body.name);
      if (!name) {
        return res.status(400).json({ error: 'name cannot be empty.' });
      }
    }

    const hasEmail =
      req.body.email !== undefined || Object.prototype.hasOwnProperty.call(req.body, 'email');
    let email;
    if (hasEmail) {
      email = normalizeEmail(req.body.email);
      if (email === undefined) {
        return res.status(400).json({ error: 'email is not a valid email address.' });
      }
    }

    const client = await updateClient(req.user.uid, req.params.id, {
      name: hasName ? name : undefined,
      email: hasEmail ? email : undefined,
      phoneNumber: req.body.phone_number ?? req.body.phoneNumber,
      address: req.body.address,
      notes: req.body.notes,
    });

    if (!client) {
      return res.status(404).json({ error: 'Client not found.' });
    }
    res.json(clientPayload(client));
  } catch (error) {
    if (isInvalidIdError(error)) {
      return res.status(404).json({ error: 'Client not found.' });
    }
    next(error);
  }
});

/**
 * DELETE /clients/:id
 * Responds 200 with a small JSON body rather than a bare 204: some HTTP clients
 * (Retrofit's Gson converter included) throw trying to parse an empty response body.
 */
router.delete('/:id', requireAuth, async (req, res, next) => {
  try {
    const deleted = await deleteClient(req.user.uid, req.params.id);
    if (!deleted) {
      return res.status(404).json({ error: 'Client not found.' });
    }
    res.status(200).json({ deleted: true });
  } catch (error) {
    if (isInvalidIdError(error)) {
      return res.status(404).json({ error: 'Client not found.' });
    }
    next(error);
  }
});

module.exports = router;
