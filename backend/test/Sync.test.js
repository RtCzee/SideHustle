const test = require('node:test');
const assert = require('node:assert/strict');
const { applyOperation } = require('../src/routes/sync');
const { withTransaction } = require('../src/db/pool');
const { createApp } = require('../src/app');

const user = { uid: 'user-a' };
const operation = {
  operation_id: 'e974b132-1c45-4b97-a21b-0dd16d31973c',
  record_id: '44be7377-c163-408e-970c-0aa30cd129f8',
  kind: 'clients',
  action: 'create',
  payload: { name: '  Studio  ', email: 'client@example.test' },
};

function database() {
  const commands = [];
  const receipts = new Map();
  const client = {
    async query(sql, values) {
      commands.push(sql);
      if (sql.startsWith('SELECT response')) {
        const response = receipts.get(values.join(':'));
        return { rows: response ? [{ response }] : [] };
      }
      if (sql.startsWith('INSERT INTO clients')) {
        return { rows: [{ client_id: values[0], user_id: values[1], name: values[2], email: values[3] }] };
      }
      if (sql.startsWith('INSERT INTO sync_operations')) {
        receipts.set(values.slice(0, 2).join(':'), values[2]);
      }
      return { rows: [] };
    },
    release() {},
  };
  return { commands, transact: work => withTransaction(work, { connect: async () => client }) };
}

test('replaying an acknowledged operation returns the original result without inserting twice', async () => {
  const { commands, transact } = database();
  const created = await applyOperation(user, operation, transact);
  const retry = await applyOperation(user, operation, transact);
  assert.equal(created.client_id, operation.record_id);
  assert.equal(created.name, 'Studio');
  assert.deepEqual(retry, created);
  assert.equal(commands.filter(sql => sql.startsWith('INSERT INTO clients')).length, 1);
  assert.equal(commands.filter(sql => sql.startsWith('INSERT INTO sync_operations')).length, 1);
  assert.equal(commands.filter(sql => sql === 'COMMIT').length, 2);
});

test('queued writes use normal validation and a rejection does not leave an acknowledgement', async () => {
  const { commands, transact } = database();
  const rejected = { ...operation, payload: { name: 'Studio', email: 'invalid' } };
  await assert.rejects(applyOperation(user, rejected, transact), { status: 400 });
  assert.equal(commands.at(-1), 'ROLLBACK');
  assert.ok(!commands.some(sql => sql.startsWith('INSERT INTO')));
});

test('profile replay rejects a record belonging to a different authenticated account', async () => {
  const { commands, transact } = database();
  await assert.rejects(applyOperation(user, { ...operation, kind: 'me', record_id: 'user-b' }, transact),
    { status: 400 });
  assert.deepEqual(commands, []);
});

test('income, invoice and sync routes are mounted and require authentication', async () => {
  const server = createApp().listen(0, '127.0.0.1');
  await new Promise((resolve, reject) => server.once('listening', resolve).once('error', reject));
  try {
    const url = 'http://127.0.0.1:' + server.address().port;
    for (const [path, method] of [['/income', 'GET'], ['/invoices', 'GET'], ['/sync', 'POST']]) {
      assert.equal((await fetch(url + path, { method })).status, 401);
    }
  } finally {
    await new Promise(resolve => server.close(resolve));
  }
});
