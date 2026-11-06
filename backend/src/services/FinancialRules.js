/** Pure rules shared by the API's dashboard and invoice creation paths. */
function calculateProfit(totalIncome, totalExpenses) {
  // Stored currency amounts have two decimal places. Subtract integer cents so a
  // result such as 0.30 - 0.20 is exactly 0.10, and preserve losses rather than clamping.
  return (Math.round(Number(totalIncome) * 100) - Math.round(Number(totalExpenses) * 100)) / 100;
}

/** Phase 2 invoices bill one selected job at its full stored agreed amount. No tax/fees. */
function invoiceTotal(agreedAmount) {
  const amount = Number(agreedAmount);
  if ((typeof agreedAmount !== 'number' && typeof agreedAmount !== 'string') ||
      (typeof agreedAmount === 'string' && agreedAmount.trim() === '') ||
      !Number.isFinite(amount) || amount < 0) {
    throw new RangeError('An invoice requires a finite, non-negative agreed amount.');
  }
  return amount;
}

function isIsoDate(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value) || value.startsWith('0000')) return false;
  const date = new Date(value + 'T00:00:00.000Z');
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

module.exports = { calculateProfit, invoiceTotal, isIsoDate };
