const ALLOWED_STATUSES = Object.freeze(['Pending', 'In Progress', 'Completed', 'Cancelled']);

function normalizeStatus(value) {
  return ALLOWED_STATUSES.includes(value) ? value : undefined;
}

function statusForNewJob(value) {
  return value === undefined ? 'Pending' : normalizeStatus(value);
}

module.exports = { ALLOWED_STATUSES, normalizeStatus, statusForNewJob };
