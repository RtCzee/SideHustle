const test = require('node:test');
const assert = require('node:assert/strict');
const { calculateProfit, invoiceTotal } = require('../src/services/FinancialRules');

for (const [income, expenses, expected] of [
  [0, 0, 0], [1000, 250, 750], [250, 1000, -750], [500, 500, 0],
  [0, 99.99, -99.99], [99.99, 0, 99.99],
  [0.30, 0.20, 0.10], [0.20, 0.30, -0.10], [1234.56, 234.55, 1000.01],
  ['1500.50', '500.25', 1000.25], [9999999999.99, 0.01, 9999999999.98],
]) {
  test(`profit: income ${income} minus expenses ${expenses} is ${expected}`, () => {
    assert.equal(calculateProfit(income, expenses), expected);
  });
}

for (const [agreedAmount, expected] of [[0, 0], [0.01, 0.01], [1250.75, 1250.75], ['2500.50', 2500.50]]) {
  test(`invoice bills the full agreed job amount ${agreedAmount} without added fees`, () => {
    assert.equal(invoiceTotal(agreedAmount), expected);
  });
}

for (const invalid of [-0.01, -100, NaN, Infinity, -Infinity, '', ' ', 'not money', null, undefined, true]) {
  test(`invoice rejects invalid agreed amount ${String(invalid)}`, () => {
    assert.throws(() => invoiceTotal(invalid), RangeError);
  });
}
