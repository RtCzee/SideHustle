const test = require('node:test');
const assert = require('node:assert/strict');
const { SideHustleScoreCalculator } = require('../src/services/SideHustleScoreCalculator');

test('a new user with no data receives score 0', () => {
  assert.equal(SideHustleScoreCalculator.calculate({}), 0);
});

test('income, expenses, jobs, and unpaid invoices each affect the score', () => {
  const healthy = SideHustleScoreCalculator.calculate({
    totalIncome: 6000, totalExpenses: 1000, completedJobs: 3, outstandingPayments: 0,
  });
  assert.equal(healthy, 100);
  assert.ok(SideHustleScoreCalculator.calculate({ totalIncome: 6000, totalExpenses: 4500, completedJobs: 3, outstandingPayments: 0 }) < healthy);
  assert.ok(SideHustleScoreCalculator.calculate({ totalIncome: 6000, totalExpenses: 1000, completedJobs: 0, outstandingPayments: 0 }) < healthy);
  assert.ok(SideHustleScoreCalculator.calculate({ totalIncome: 6000, totalExpenses: 1000, completedJobs: 3, outstandingPayments: 6000 }) < healthy);
});

test('score remains within the 0-100 range', () => {
  assert.equal(SideHustleScoreCalculator.calculate({ totalIncome: 100000, completedJobs: 99 }), 100);
  assert.equal(SideHustleScoreCalculator.calculate({ totalIncome: 0, totalExpenses: 1000, completedJobs: -1 }), 0);
});

// Isolate each component so a wrong weight cannot hide behind the overall 100-point cap.
for (const [income, expenses, expected] of [[100, 0, 1], [4500, 2000, 25], [5000, 0, 50], [9000, 0, 50], [100, 500, 0]]) {
  test(`profit score: income ${income}, expenses ${expenses} contributes ${expected}`, () => {
    assert.equal(SideHustleScoreCalculator.calculate({
      totalIncome: income, totalExpenses: expenses, completedJobs: 0, outstandingPayments: income,
    }), expected);
  });
}

for (const [jobs, expected] of [[0, 0], [1, 10], [2, 20], [3, 30], [4, 30], [100, 30]]) {
  test(`${jobs} completed jobs contributes ${expected} with the 30-point cap`, () => {
    assert.equal(SideHustleScoreCalculator.calculate({ completedJobs: jobs }), expected);
  });
}

for (const [outstanding, expected] of [[0, 20], [25, 15], [50, 10], [100, 0], [150, 0]]) {
  test(`payment score with income 100 and outstanding ${outstanding} is ${expected}`, () => {
    assert.equal(SideHustleScoreCalculator.calculate({
      totalIncome: 100, totalExpenses: 100, outstandingPayments: outstanding,
    }), expected);
  });
}

test('all three uncapped components add to an independently calculated score', () => {
  // R1500 profit -> 15; two completed jobs -> 20; 75% of income collected -> 15.
  assert.equal(SideHustleScoreCalculator.calculate({
    totalIncome: 2500, totalExpenses: 1000, completedJobs: 2, outstandingPayments: 625,
  }), 50);
});

test('a business loss does not subtract earned job points', () => {
  assert.equal(SideHustleScoreCalculator.calculate({
    totalIncome: 100, totalExpenses: 500, completedJobs: 2, outstandingPayments: 100,
  }), 20);
});

test('fractional points round once at the half-point boundary', () => {
  assert.equal(SideHustleScoreCalculator.calculate({ totalIncome: 49, outstandingPayments: 49 }), 0);
  assert.equal(SideHustleScoreCalculator.calculate({ totalIncome: 50, outstandingPayments: 50 }), 1);
  assert.equal(SideHustleScoreCalculator.calculate({ totalIncome: 1000, totalExpenses: 1000, outstandingPayments: 125 }), 18);
});

test('no income avoids division by zero and awards no payment points', () => {
  assert.equal(SideHustleScoreCalculator.calculate({ totalIncome: 0, outstandingPayments: 1000, completedJobs: 1 }), 10);
});

test('database numeric strings are handled as numbers', () => {
  assert.equal(SideHustleScoreCalculator.calculate({
    totalIncome: '2500', totalExpenses: '1000', completedJobs: '2', outstandingPayments: '625',
  }), 50);
});

test('missing negative and nonfinite inputs normalize safely', () => {
  for (const value of [undefined, null, '', -1, NaN, Infinity, -Infinity, 'not a number']) {
    assert.equal(SideHustleScoreCalculator.calculate({
      totalIncome: value, totalExpenses: value, completedJobs: value, outstandingPayments: value,
    }), 0, `input ${String(value)}`);
  }
});

test('scores stay finite integers from 0 to 100 across representative business states', () => {
  for (const income of [0, 1, 100, 5000, 10000]) {
    for (const expenses of [0, 2000, 11000]) {
      for (const jobs of [0, 1, 3, 10]) {
        for (const outstanding of [0, 100, 20000]) {
          const score = SideHustleScoreCalculator.calculate({
            totalIncome: income, totalExpenses: expenses, completedJobs: jobs, outstandingPayments: outstanding,
          });
          assert.ok(Number.isInteger(score) && score >= 0 && score <= 100);
        }
      }
    }
  }
});
