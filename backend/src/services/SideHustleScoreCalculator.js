/**
 * Converts the core financial metrics into a stable 0-100 SideHustle Score.
 * Keeping this pure class independent of the database makes it reusable and testable.
 */
class SideHustleScoreCalculator {
  static calculate({ totalIncome, totalExpenses, completedJobs, outstandingPayments }) {
    const income = SideHustleScoreCalculator.numberOrZero(totalIncome);
    const expenses = SideHustleScoreCalculator.numberOrZero(totalExpenses);
    const jobs = SideHustleScoreCalculator.numberOrZero(completedJobs);
    const outstanding = SideHustleScoreCalculator.numberOrZero(outstandingPayments);

    const profitPoints = Math.min(50, Math.max(0, (income - expenses) / 100));
    const jobPoints = Math.min(30, jobs * 10);
    const paymentPoints = income > 0
      ? Math.min(20, Math.max(0, 20 * (1 - outstanding / income)))
      : 0;

    return Math.round(Math.min(100, Math.max(0, profitPoints + jobPoints + paymentPoints)));
  }

  static numberOrZero(value) {
    const number = Number(value);
    return Number.isFinite(number) && number > 0 ? number : 0;
  }
}

module.exports = { SideHustleScoreCalculator };
