const express = require('express');
const { requireAuth } = require('../middleware/auth');
const { getPool } = require('../db/pool');
const { admin } = require('../config/firebase');
const { ENTITIES, MAX_FILE_SIZE, validAttachment } = require('../services/AttachmentRules');
function createAttachmentsRouter({ auth = requireAuth, query = (...args) => getPool().query(...args),
  metadataFor = async (bucket, path) => {
    const [metadata] = await admin.storage().bucket(bucket).file(path).getMetadata();
    return metadata;
  }, bucketName = () => process.env.FIREBASE_STORAGE_BUCKET } = {}) {
const router = express.Router();

router.use('/:type/:id', auth, async (req, res, next) => {
  const entity = Object.hasOwn(ENTITIES, req.params.type) ? ENTITIES[req.params.type] : null;
  if (!entity || !/^[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}$/i.test(req.params.id)) {
    return res.status(404).json({ error: 'Record not found.' });
  }
  try {
    const { rows } = await query(
      `SELECT 1 FROM ${entity.table} WHERE ${entity.key} = $1 AND user_id = $2`, [req.params.id, req.user.uid]);
    if (!rows.length) return res.status(404).json({ error: 'Record not found.' });
    req.attachmentEntity = entity;
    next();
  } catch (error) { next(error); }
});

router.get('/:type/:id', async (req, res, next) => {
  try {
    const { rows } = await query(
      `SELECT * FROM attachments WHERE ${req.attachmentEntity.key} = $1 AND user_id = $2 ORDER BY created_at`,
      [req.params.id, req.user.uid]);
    res.json(rows);
  } catch (error) { next(error); }
});

router.post('/:type/:id', async (req, res, next) => {
  try {
    const bucket = bucketName();
    if (!bucket) return res.status(503).json({ error: 'Attachment storage is not configured.' });
    if (!validAttachment(req.body, req.user.uid, req.params.type, req.params.id, bucket)) {
      return res.status(400).json({ error: 'Invalid attachment URL, path or name.' });
    }
    // Confirm that this is an uploaded object, rather than trusting a client URL.
    let metadata;
    try {
      metadata = await metadataFor(bucket, req.body.storage_path);
    } catch (error) {
      if (error.code === 404) return res.status(400).json({ error: 'Upload the file before saving its attachment.' });
      throw error;
    }
    const token = new URL(req.body.url).searchParams.get('token');
    if (Number(metadata.size) <= 0 || Number(metadata.size) > MAX_FILE_SIZE ||
        !(metadata.metadata?.firebaseStorageDownloadTokens || '').split(',').includes(token)) {
      return res.status(400).json({ error: 'Invalid uploaded file or download token.' });
    }
    const { rows } = await query(
      `INSERT INTO attachments(user_id, ${req.attachmentEntity.key}, name, url, storage_path)
       VALUES ($1, $2, $3, $4, $5)
       ON CONFLICT (storage_path) DO UPDATE SET url = EXCLUDED.url
       RETURNING *`,
      [req.user.uid, req.params.id, req.body.name.trim(), req.body.url, req.body.storage_path]);
    res.status(201).json(rows[0]);
  } catch (error) { next(error); }
});
return router;
}
module.exports = createAttachmentsRouter();
module.exports.createAttachmentsRouter = createAttachmentsRouter;
