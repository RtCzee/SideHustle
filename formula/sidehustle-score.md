# SideHustle Score formula

The SideHustle Score is a whole number from **0 to 100**. It gives the user a quick view of business health while keeping the calculation simple enough to explain and test.

```
score = round(clamp(0, 100, profitPoints + jobPoints + paymentPoints))

profitPoints  = clamp(0, 50, (totalIncome - totalExpenses) / 100)
jobPoints     = clamp(0, 30, completedJobsThisMonth * 10)
paymentPoints = totalIncome > 0
  ? clamp(0, 20, 20 * (1 - outstandingPayments / totalIncome))
  : 0
```

`clamp(min, max, value)` limits a value to the stated range. Currency amounts are in the user's stored business currency; the current app defaults to ZAR.

## Why these weights

- **Profit: 50 points.** Income increases the score and expenses reduce it, so profitable work has the largest influence.
- **Completed jobs: 30 points.** Each completed job in the current month contributes 10 points, up to three jobs.
- **Outstanding payments: 20 points.** The score rewards collecting payments. If outstanding payments equal or exceed income, this component is zero.

A new account has no income, expenses, completed jobs, or outstanding payments, so its score is safely **0**. The calculator is a pure server-side class and is covered by automated unit tests.
