const test = require('node:test');
const assert = require('node:assert/strict');
const { sendJobReminders, startReminderWorker } = require('../src/services/JobReminderSender');
const { updateUser, findUserById } = require('../src/db/users');
const { registerDevice } = require('../src/db/pushDevices');

const reminder = {
  user_id: 'alice', job_id: '11111111-1111-4111-8111-111111111111', title: 'Build a desk',
  due_date: '2026-11-06', device_id: '22222222-2222-4222-8222-222222222222', fcm_token: 'device-token', claim_id: 'claim',
};

function fixture({ candidates = [reminder], claim = reminder, error } = {}) {
  const calls = [];
  const messages = [];
  let delivered = false;
  return {
    calls, messages,
    options: {
      store: {
        candidates: async () => delivered ? [] : candidates,
        claim: async () => claim,
        sent: async () => { delivered = true; calls.push('sent'); },
        release: async () => { calls.push('release'); },
        removeInvalidDevice: async () => { calls.push('remove'); },
      },
      send: async message => { messages.push(message); if (error) throw error; return 'message-id'; },
      log: { warn() {} },
    },
  };
}

test('due job sends background data push with account and job navigation target', async () => {
  const f = fixture();
  assert.deepEqual(await sendJobReminders(f.options), { sent: 1, failed: 0, skipped: 0 });
  assert.deepEqual(f.messages[0], {
    token: 'device-token',
    data: { type: 'job_due', user_id: 'alice', job_id: reminder.job_id,
      title: 'Job due soon', body: 'Build a desk is due on 2026-11-06.' },
    android: { priority: 'high', ttl: 3600000 },
  });
  assert.deepEqual(f.calls, ['sent']);
  assert.deepEqual(await sendJobReminders(f.options), { sent: 0, failed: 0, skipped: 0 });
  assert.equal(f.messages.length, 1);
});

test('no registered devices or due jobs does not call FCM', async () => {
  const f = fixture({ candidates: [] });
  assert.deepEqual(await sendJobReminders(f.options), { sent: 0, failed: 0, skipped: 0 });
  assert.equal(f.messages.length, 0);
});

test('missing tokens and a failed eligibility claim are skipped safely', async () => {
  for (const token of [null, undefined, '', ' ']) {
    const f = fixture({ candidates: [{ ...reminder, fcm_token: token }] });
    assert.equal((await sendJobReminders(f.options)).skipped, 1);
    assert.equal(f.messages.length, 0);
  }
  // Claim rechecks ownership, active status, due date and settings. An opted-out job yields no claim.
  const f = fixture({ claim: null });
  assert.equal((await sendJobReminders(f.options)).skipped, 1);
  assert.equal(f.messages.length, 0);
});

test('transient FCM failure releases claim without marking delivered so next run retries', async () => {
  const f = fixture({ error: Object.assign(new Error('offline'), { code: 'messaging/server-unavailable' }) });
  assert.equal((await sendJobReminders(f.options)).failed, 1);
  assert.deepEqual(f.calls, ['release']);
  f.options.send = async message => { f.messages.push(message); };
  assert.equal((await sendJobReminders(f.options)).sent, 1);
  assert.deepEqual(f.calls, ['release', 'sent']);
});

for (const code of ['messaging/registration-token-not-registered', 'messaging/invalid-registration-token']) {
  test(`expired token is removed: ${code}`, async () => {
    const f = fixture({ error: Object.assign(new Error('expired'), { code }) });
    assert.equal((await sendJobReminders(f.options)).failed, 1);
    assert.deepEqual(f.calls, ['remove']);
  });
}

test('worker contains database errors and continues on the next tick', async t => {
  let attempts = 0;
  let errors = 0;
  const succeeded = new Promise(resolve => {
    const stop = startReminderWorker({ intervalMs: 10,
      run: async () => { if (++attempts === 1) throw new Error('database offline'); resolve(); },
      log: { error: () => errors++ },
    });
    t.after(stop);
  });
  // Keep this test alive while the production timer is unref'ed.
  const timeout = setTimeout(() => {}, 1000);
  t.after(() => clearTimeout(timeout));
  await succeeded;
  assert.equal(errors, 1);
  assert.ok(attempts >= 2);
});

test('profile update persists notifications false transactionally and reads the saved setting', async () => {
  const calls = [];
  const client = {
    query: async (sql, values) => {
      calls.push({ sql, values });
      return { rows: sql.startsWith('SELECT') ? [{ user_id: 'alice', notifications_enabled: false }] : [] };
    },
    release: () => calls.push({ sql: 'RELEASE' }),
  };
  const profile = await updateUser('alice', { notificationsEnabled: false }, { connect: async () => client });
  assert.equal(profile.notifications_enabled, false);
  assert.deepEqual(calls.find(c => c.sql.includes('INSERT INTO user_settings')).values, ['alice', false]);
  assert.match(calls.find(c => c.sql.startsWith('SELECT')).sql, /notifications_enabled FROM user_settings/);
  assert.equal(calls.at(-2).sql, 'COMMIT');
  assert.equal(calls.at(-1).sql, 'RELEASE');
});

test('profile query includes saved notifications preference', async () => {
  const result = await findUserById('alice', { query: async (sql, values) => {
    assert.match(sql, /user_settings.user_id = users.user_id/);
    assert.deepEqual(values, ['alice']);
    return { rows: [{ user_id: 'alice', notifications_enabled: false }] };
  } });
  assert.equal(result.notifications_enabled, false);
});

test('token registration replaces owner and token for an installation', async () => {
  const calls = [];
  const client = { query: async (sql, values) => { calls.push({ sql, values }); }, release() {} };
  await registerDevice('bob', { deviceId: reminder.device_id, token: 'rotated-token', enabled: false },
    { connect: async () => client });
  const insert = calls.find(c => c.sql.includes('INSERT INTO push_devices'));
  assert.deepEqual(insert.values, [reminder.device_id, 'bob', 'rotated-token', false]);
  assert.match(insert.sql, /user_id = EXCLUDED.user_id/);
  assert.match(insert.sql, /fcm_token = EXCLUDED.fcm_token/);
  assert.equal(calls.at(-1).sql, 'COMMIT');
});
