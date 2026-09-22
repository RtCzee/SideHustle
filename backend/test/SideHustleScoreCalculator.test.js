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
