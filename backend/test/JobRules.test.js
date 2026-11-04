const test = require('node:test');
const assert = require('node:assert/strict');
const { ALLOWED_STATUSES, normalizeStatus, statusForNewJob } = require('../src/services/JobRules');

test('the allowed job states match the business vocabulary exactly', () => {
  assert.deepEqual(ALLOWED_STATUSES, ['Pending', 'In Progress', 'Completed', 'Cancelled']);
});

for (const status of ['Pending', 'In Progress', 'Completed', 'Cancelled']) {
  test(`creation and updates preserve the valid state ${status}`, () => {
    assert.equal(normalizeStatus(status), status);
    assert.equal(statusForNewJob(status), status);
  });
}

test('only an omitted create status defaults to Pending', () => {
  assert.equal(statusForNewJob(undefined), 'Pending');
  assert.equal(normalizeStatus(undefined), undefined);
});

for (const invalid of ['', ' ', 'completed', 'InProgress', 'Done', 'Paid', ' Completed ', null, 1, {}, []]) {
  test(`invalid status ${JSON.stringify(invalid)} is rejected rather than silently defaulted`, () => {
    assert.equal(normalizeStatus(invalid), undefined);
    assert.equal(statusForNewJob(invalid), undefined);
  });
}
