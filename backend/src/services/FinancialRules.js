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

module.exports = { calculateProfit, invoiceTotal };
