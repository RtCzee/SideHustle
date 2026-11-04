const test = require('node:test');
const assert = require('node:assert/strict');
const express = require('express');
const { createAttachmentsRouter } = require('../src/routes/attachments');
const { validAttachment } = require('../src/services/AttachmentRules');
const id = '11111111-1111-4111-8111-111111111111';
const bucket = 'test.firebasestorage.app';
const bodyFor = (type = 'jobs') => {
  const path = `attachments/alice/${type}/${id}/22222222-2222-4222-8222-222222222222`;
  return { name: 'Receipt.pdf', storage_path: path,
    url: `https://firebasestorage.googleapis.com/v0/b/${bucket}/o/${encodeURIComponent(path)}?alt=media&token=secret` };
};

test('URL validation rejects other owners, entities, buckets and arbitrary URLs', () => {
  const body = bodyFor();
  assert.equal(validAttachment(body, 'alice', 'jobs', id, bucket), true);
  assert.equal(validAttachment(body, 'bob', 'jobs', id, bucket), false);
  assert.equal(validAttachment(body, 'alice', 'expenses', id, bucket), false);
  assert.equal(validAttachment(body, 'alice', 'jobs', id, 'other'), false);
  for (const url of ['https://example.com/file', 'javascript:alert(1)', body.url.replace('https:', 'http:'),
    body.url.replace('googleapis.com', 'googleapis.com.evil.test'), body.url.replace('token=secret', 'token=')]) {
    assert.equal(validAttachment({ ...body, url }, 'alice', 'jobs', id, bucket), false);
  }
});

async function fixture(t, { owns = true, metadata = { size: '123', metadata: { firebaseStorageDownloadTokens: 'secret' } } } = {}) {
  const calls = [];
  const app = express();
  app.use(express.json());
  app.use('/attachments', createAttachmentsRouter({
    auth: (req, res, next) => { req.user = { uid: 'alice' }; next(); },
    bucketName: () => bucket,
    metadataFor: async () => { if (metadata instanceof Error) throw metadata; return metadata; },
    query: async (sql, values) => {
      calls.push({ sql, values });
      if (sql.startsWith('SELECT 1')) return { rows: owns ? [{}] : [] };
      if (sql.includes('INSERT')) return { rows: [{ attachment_id: id, url: values[3], storage_path: values[4] }] };
      return { rows: [{ name: 'Receipt.pdf', url: bodyFor().url }] };
    },
  }));
  const server = app.listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  t.after(() => new Promise(resolve => { server.close(resolve); server.closeAllConnections(); }));
  return { calls, request: (type = 'jobs', method = 'POST', body = bodyFor(type)) =>
    fetch(`http://127.0.0.1:${server.address().port}/attachments/${type}/${id}`, {
      method, headers: { 'Content-Type': 'application/json' }, body: method === 'POST' ? JSON.stringify(body) : undefined,
    }) };
}

for (const type of ['jobs', 'expenses']) {
  test(`${type}: persists URL with owner and entity foreign key after checking Storage`, async t => {
    const f = await fixture(t);
    const response = await f.request(type);
    assert.equal(response.status, 201);
    assert.equal((await response.json()).url, bodyFor(type).url);
    const insert = f.calls.find(c => c.sql.includes('INSERT'));
    assert.match(insert.sql, new RegExp(type === 'jobs' ? 'job_id' : 'expense_id'));
    assert.deepEqual(insert.values.slice(0, 2), ['alice', id]);
    assert.equal(insert.values[3], bodyFor(type).url);
    assert.match(insert.sql, /ON CONFLICT \(storage_path\)/);
  });
  test(`${type}: another account's record cannot be read or written`, async t => {
    const f = await fixture(t, { owns: false });
    assert.equal((await f.request(type)).status, 404);
    assert.equal((await f.request(type, 'GET')).status, 404);
    assert.equal(f.calls.length, 2);
    assert.ok(f.calls.every(c => c.sql.startsWith('SELECT 1')));
  });
}

test('lists saved URLs for owned entity', async t => {
  const f = await fixture(t);
  const response = await f.request('jobs', 'GET');
  assert.equal(response.status, 200);
  assert.equal((await response.json())[0].url, bodyFor().url);
  assert.deepEqual(f.calls[1].values, [id, 'alice']);
});

for (const metadata of [{ size: '0' }, { size: String(21 * 1024 * 1024) },
  { size: '123', metadata: { firebaseStorageDownloadTokens: 'wrong' } }, Object.assign(new Error('missing'), { code: 404 })]) {
  test(`rejects missing, empty, oversized or invalid token object: ${JSON.stringify(metadata)}`, async t => {
    const f = await fixture(t, { metadata });
    assert.equal((await f.request()).status, 400);
    assert.equal(f.calls.length, 1);
  });
}
