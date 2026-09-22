const { getPool } = require('./pool');
const { findClientById } = require('./clients');
const { findUserById } = require('./users');

const JOB_COLUMNS = `
  j.job_id, j.user_id, j.client_id, c.name AS client_name,
  j.title, j.description, j.status,
  j.start_date, j.due_date, j.completed_date,
  j.agreed_amount, j.currency,
  j.created_at, j.updated_at
`;

// Joins clients so the list/details screens get the client's name without a second
// round trip (issue: list must show title, client, status, due date).
const JOB_QUERY = `
  SELECT ${JOB_COLUMNS}
  FROM jobs j
  JOIN clients c ON c.client_id = j.client_id
`;

/** All jobs for a user, most recently created first. */
async function listJobs(userId) {
  const result = await getPool().query(
    `${JOB_QUERY} WHERE j.user_id = $1 ORDER BY j.created_at DESC`,
    [userId]
  );
  return result.rows;
}

/** A single job, scoped to the owning user. */
async function findJobById(userId, jobId) {
  const result = await getPool().query(
    `${JOB_QUERY} WHERE j.user_id = $1 AND j.job_id = $2`,
    [userId, jobId]
  );
  return result.rows[0] ?? null;
}

/** True if this client_id exists and is owned by userId — used to stop a job being
 *  linked to another user's client. */
async function userOwnsClient(userId, clientId) {
  const client = await findClientById(userId, clientId);
  return client !== null;
}

async function createJob(userId, {
  clientId,
  title,
  description,
  status,
  startDate,
  dueDate,
  completedDate,
  agreedAmount,
  currency,
}) {
  let resolvedCurrency = currency;
  if (!resolvedCurrency) {
    const profile = await findUserById(userId);
    resolvedCurrency = profile?.preferred_currency ?? 'ZAR';
  }

  const result = await getPool().query(
    `INSERT INTO jobs (
       user_id, client_id, title, description, status,
       start_date, due_date, completed_date, agreed_amount, currency
     )
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
     RETURNING job_id`,
    [
      userId,
      clientId,
      title,
      description ?? null,
      status ?? 'Pending',
      startDate ?? null,
      dueDate ?? null,
      completedDate ?? null,
      agreedAmount ?? 0,
      resolvedCurrency,
    ]
  );

  return findJobById(userId, result.rows[0].job_id);
}

/** Updates whichever fields are provided (undefined = leave unchanged). Returns null if not found/not owned. */
async function updateJob(userId, jobId, fields) {
  const columns = {
    client_id: fields.clientId,
    title: fields.title,
    description: fields.description,
    status: fields.status,
    start_date: fields.startDate,
    due_date: fields.dueDate,
    completed_date: fields.completedDate,
    agreed_amount: fields.agreedAmount,
    currency: fields.currency,
  };

  const sets = [];
  const values = [];
  for (const [column, value] of Object.entries(columns)) {
    if (value !== undefined) {
      sets.push(`${column} = $${sets.length + 1}`);
      values.push(value);
    }
  }

  if (sets.length === 0) {
    return findJobById(userId, jobId);
  }

  sets.push('updated_at = NOW()');
  values.push(userId, jobId);

  const result = await getPool().query(
    `UPDATE jobs SET ${sets.join(', ')}
     WHERE user_id = $${values.length - 1} AND job_id = $${values.length}
     RETURNING job_id`,
    values
  );

  if (result.rows.length === 0) {
    return null;
  }
  return findJobById(userId, jobId);
}

module.exports = { listJobs, findJobById, createJob, updateJob, userOwnsClient };
