const express = require('express');
const { requireAuth } = require('../middleware/auth');
const { listJobs, findJobById, createJob, updateJob, userOwnsClient } = require('../db/jobs');

const router = express.Router();

const ALLOWED_STATUSES = ['Pending', 'In Progress', 'Completed', 'Cancelled'];
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

function normalizeTitle(value) {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function normalizeStatus(value) {
  return ALLOWED_STATUSES.includes(value) ? value : undefined;
}

/** Returns a finite number >= 0, or undefined if the value isn't a valid amount. */
function normalizeAmount(value) {
  if (typeof value !== 'number' && typeof value !== 'string') return undefined;
  const amount = Number(value);
  if (!Number.isFinite(amount) || amount < 0) return undefined;
  return Math.round(amount * 100) / 100;
}

/** Returns the date string if valid, null if blank/omitted (clears the date), or undefined if malformed. */
function normalizeDate(value) {
  if (value === undefined || value === null) return null;
  if (typeof value !== 'string') return undefined;
  const trimmed = value.trim();
  if (trimmed.length === 0) return null;
  return DATE_PATTERN.test(trimmed) ? trimmed : undefined;
}

function jobPayload(row) {
  return {
    job_id: row.job_id,
    user_id: row.user_id,
    client_id: row.client_id,
    client_name: row.client_name,
    title: row.title,
    description: row.description,
    status: row.status,
    start_date: row.start_date,
    due_date: row.due_date,
    completed_date: row.completed_date,
    agreed_amount: Number(row.agreed_amount),
    currency: row.currency,
    created_at: row.created_at,
    updated_at: row.updated_at,
  };
}

// A malformed UUID in :id or :clientId makes Postgres throw invalid_text_representation (22P02).
function isInvalidIdError(error) {
  return error && error.code === '22P02';
}

/** GET /jobs — list, most recent first. */
router.get('/', requireAuth, async (req, res, next) => {
  try {
    const rows = await listJobs(req.user.uid);
    res.json(rows.map(jobPayload));
  } catch (error) {
    next(error);
  }
});

/** POST /jobs — create. Requires an existing client owned by this user. */
router.post('/', requireAuth, async (req, res, next) => {
  try {
    const title = normalizeTitle(req.body.title);
    if (!title) {
      return res.status(400).json({ error: 'title is required.' });
    }

    const clientId = req.body.client_id ?? req.body.clientId;
    if (!clientId) {
      return res.status(400).json({ error: 'client_id is required.' });
    }

    let ownsClient;
    try {
      ownsClient = await userOwnsClient(req.user.uid, clientId);
    } catch (error) {
      if (isInvalidIdError(error)) {
        return res.status(400).json({ error: 'client_id is not a valid client.' });
      }
      throw error;
    }
    if (!ownsClient) {
      return res.status(400).json({ error: 'client_id must be one of your existing clients.' });
    }

    const status = req.body.status === undefined ? 'Pending' : normalizeStatus(req.body.status);
    if (status === undefined) {
      return res.status(400).json({ error: `status must be one of: ${ALLOWED_STATUSES.join(', ')}.` });
    }

    const agreedAmount = req.body.agreed_amount ?? req.body.agreedAmount ?? 0;
    const amount = normalizeAmount(agreedAmount);
    if (amount === undefined) {
      return res.status(400).json({ error: 'agreed_amount must be a number greater than or equal to 0.' });
    }

    const startDate = normalizeDate(req.body.start_date ?? req.body.startDate);
    const dueDate = normalizeDate(req.body.due_date ?? req.body.dueDate);
    const completedDate = normalizeDate(req.body.completed_date ?? req.body.completedDate);
    if (startDate === undefined || dueDate === undefined || completedDate === undefined) {
      return res.status(400).json({ error: 'Dates must be in YYYY-MM-DD format.' });
    }

    const job = await createJob(req.user.uid, {
      clientId,
      title,
      description: req.body.description ?? null,
      status,
      startDate,
      dueDate,
      completedDate,
      agreedAmount: amount,
      currency: req.body.currency ?? undefined,
    });

    res.status(201).json(jobPayload(job));
  } catch (error) {
    next(error);
  }
});

/** GET /jobs/:id */
router.get('/:id', requireAuth, async (req, res, next) => {
  try {
    const job = await findJobById(req.user.uid, req.params.id);
    if (!job) {
      return res.status(404).json({ error: 'Job not found.' });
    }
    res.json(jobPayload(job));
  } catch (error) {
    if (isInvalidIdError(error)) {
      return res.status(404).json({ error: 'Job not found.' });
    }
    next(error);
  }
});

/** PUT /jobs/:id — partial update; only provided fields change. Status can be changed here. */
router.put('/:id', requireAuth, async (req, res, next) => {
  try {
    const hasTitle = req.body.title !== undefined;
    let title;
    if (hasTitle) {
      title = normalizeTitle(req.body.title);
      if (!title) {
        return res.status(400).json({ error: 'title cannot be empty.' });
      }
    }

    const rawClientId = req.body.client_id ?? req.body.clientId;
    const hasClientId = rawClientId !== undefined;
    if (hasClientId) {
      let ownsClient;
      try {
        ownsClient = await userOwnsClient(req.user.uid, rawClientId);
      } catch (error) {
        if (isInvalidIdError(error)) {
          return res.status(400).json({ error: 'client_id is not a valid client.' });
        }
        throw error;
      }
      if (!ownsClient) {
        return res.status(400).json({ error: 'client_id must be one of your existing clients.' });
      }
    }

    const hasStatus = req.body.status !== undefined;
    let status;
    if (hasStatus) {
      status = normalizeStatus(req.body.status);
      if (status === undefined) {
        return res.status(400).json({ error: `status must be one of: ${ALLOWED_STATUSES.join(', ')}.` });
      }
    }

    const rawAmount = req.body.agreed_amount ?? req.body.agreedAmount;
    const hasAmount = rawAmount !== undefined;
    let amount;
    if (hasAmount) {
      amount = normalizeAmount(rawAmount);
      if (amount === undefined) {
        return res.status(400).json({ error: 'agreed_amount must be a number greater than or equal to 0.' });
      }
    }

    const hasStartDate =
      req.body.start_date !== undefined || req.body.startDate !== undefined;
    const startDate = hasStartDate ? normalizeDate(req.body.start_date ?? req.body.startDate) : undefined;

    const hasDueDate = req.body.due_date !== undefined || req.body.dueDate !== undefined;
    const dueDate = hasDueDate ? normalizeDate(req.body.due_date ?? req.body.dueDate) : undefined;

    const hasCompletedDate =
      req.body.completed_date !== undefined || req.body.completedDate !== undefined;
    const completedDate = hasCompletedDate
      ? normalizeDate(req.body.completed_date ?? req.body.completedDate)
      : undefined;

    if (startDate === undefined && hasStartDate) {
      return res.status(400).json({ error: 'start_date must be in YYYY-MM-DD format.' });
    }
    if (dueDate === undefined && hasDueDate) {
      return res.status(400).json({ error: 'due_date must be in YYYY-MM-DD format.' });
    }
    if (completedDate === undefined && hasCompletedDate) {
      return res.status(400).json({ error: 'completed_date must be in YYYY-MM-DD format.' });
    }

    const job = await updateJob(req.user.uid, req.params.id, {
      clientId: hasClientId ? rawClientId : undefined,
      title: hasTitle ? title : undefined,
      description: req.body.description,
      status: hasStatus ? status : undefined,
      startDate: hasStartDate ? startDate : undefined,
      dueDate: hasDueDate ? dueDate : undefined,
      completedDate: hasCompletedDate ? completedDate : undefined,
      agreedAmount: hasAmount ? amount : undefined,
      currency: req.body.currency,
    });

    if (!job) {
      return res.status(404).json({ error: 'Job not found.' });
    }
    res.json(jobPayload(job));
  } catch (error) {
    if (isInvalidIdError(error)) {
      return res.status(404).json({ error: 'Job not found.' });
    }
    next(error);
  }
});

module.exports = router;
