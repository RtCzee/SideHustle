const test = require('node:test');
const assert = require('node:assert/strict');
const express = require('express');
const { createPushDevicesRouter } = require('../src/routes/pushDevices');

async function fixture(t, { profile = true, authenticated = true } = {}) {
  const saved = [];
  const app = express();
  app.use(express.json());
  app.use('/me/push-device', createPushDevicesRouter({
    auth: (req, res, next) => {
      if (!authenticated) return res.sendStatus(401);
      req.user = { uid: 'alice' }; next();
    },
    findUser: async () => profile ? { user_id: 'alice' } : null,
    save: async (...args) => saved.push(args),
  }));
  const server = app.listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  t.after(() => new Promise(resolve => { server.close(resolve); server.closeAllConnections(); }));
  return { saved, request: body => fetch(`http://127.0.0.1:${server.address().port}/me/push-device`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
  }) };
}
const valid = { device_id: '11111111-1111-4111-8111-111111111111', user_id: 'alice', token: 'fcm-token', enabled: true };

test('authenticated device registration stores token for signed-in account', async t => {
  const f = await fixture(t);
  const response = await f.request(valid);
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { registered: true });
  assert.deepEqual(f.saved[0], ['alice', { deviceId: valid.device_id, token: valid.token, enabled: true }]);
});

test('missing and malformed tokens return validation errors instead of crashing', async t => {
  const f = await fixture(t);
  for (const token of [undefined, null, '', ' ', 123]) {
    assert.equal((await f.request({ ...valid, token })).status, 400);
  }
  assert.equal(f.saved.length, 0);
});

test('does not register a token for a different account or missing profile', async t => {
  const f = await fixture(t);
  assert.equal((await f.request({ ...valid, user_id: 'bob' })).status, 403);
  assert.equal(f.saved.length, 0);
  const missing = await fixture(t, { profile: false });
  assert.equal((await missing.request(valid)).status, 404);
  const unauthenticated = await fixture(t, { authenticated: false });
  assert.equal((await unauthenticated.request(valid)).status, 401);
});
