package com.example.sidehustle.data.repository

import com.example.sidehustle.data.model.DashboardResponse
import com.example.sidehustle.data.model.ExpenseResponse
import com.example.sidehustle.data.model.IncomeResponse
import com.example.sidehustle.data.model.InvoiceResponse
import com.example.sidehustle.data.model.JobResponse
import com.example.sidehustle.data.model.UserProfileResponse
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal fun cents(amount: Double): Double = (amount * 100).roundToLong() / 100.0

/** Keep the dashboard totals and score aligned with backend/src/db/dashboard.js. */
internal fun localDashboard(
    profile: UserProfileResponse,
    income: List<IncomeResponse>,
    expenses: List<ExpenseResponse>,
    jobs: List<JobResponse>,
    invoices: List<InvoiceResponse>,
    monthStart: String,
): DashboardResponse {
    val totalIncome = cents(income.sumOf { it.amount })
    val totalExpenses = cents(expenses.sumOf { it.amount })
    val profit = cents(totalIncome - totalExpenses)
    val outstanding = cents(invoices.filter { it.status != "Draft" && it.status != "Paid" }
        .sumOf { it.total - it.amountPaid })
    val completed = jobs.count { it.status == "Completed" && (it.completedDate ?: "") >= monthStart }

    val profitPoints = (profit / 100).coerceIn(0.0, 50.0)
    val jobPoints = (completed * 10).coerceAtMost(30)
    val paymentPoints = if (totalIncome > 0) (20 * (1 - outstanding.coerceAtLeast(0.0) / totalIncome))
        .coerceIn(0.0, 20.0) else 0.0
    val score = (profitPoints + jobPoints + paymentPoints).roundToInt()
    return DashboardResponse(profile.fullName, profile.preferredCurrency, totalIncome, totalExpenses,
        profit, outstanding, completed, score)
}
