const ENTITIES = Object.freeze({
  jobs: { table: 'jobs', key: 'job_id' },
  expenses: { table: 'expense_records', key: 'expense_id' },
});
const MAX_FILE_SIZE = 20 * 1024 * 1024;

function validAttachment(body, uid, type, id, bucket) {
  if (!body || typeof body.name !== 'string' || !body.name.trim() || body.name.length > 255 ||
      typeof body.storage_path !== 'string' || typeof body.url !== 'string' || body.url.length > 8192) return false;
  const prefix = `attachments/${uid}/${type}/${id}/`;
  if (!body.storage_path.startsWith(prefix) || !/^[a-f0-9-]{36}$/.test(body.storage_path.slice(prefix.length))) return false;
  try {
    const url = new URL(body.url);
    return Boolean(bucket) && url.protocol === 'https:' && url.host === 'firebasestorage.googleapis.com' &&
      !url.username && !url.password &&
      url.pathname === `/v0/b/${bucket}/o/${encodeURIComponent(body.storage_path)}` &&
      url.searchParams.get('alt') === 'media' && Boolean(url.searchParams.get('token'));
  } catch { return false; }
}

module.exports = { ENTITIES, MAX_FILE_SIZE, validAttachment };
