package com.example.sidehustle.util

import android.content.Context
import com.example.sidehustle.R

object LocalizedLabels {
    private val jobStatuses = mapOf(
        "Pending" to R.string.job_status_pending,
        "In Progress" to R.string.job_status_in_progress,
        "Completed" to R.string.job_status_completed,
        "Cancelled" to R.string.job_status_cancelled,
    )
    private val expenseCategories = mapOf(
        "Transport" to R.string.expense_category_transport,
        "Materials" to R.string.expense_category_materials,
        "Software" to R.string.expense_category_software,
        "Other" to R.string.expense_category_other,
    )
    private val paymentMethods = mapOf(
        "Cash" to R.string.payment_method_cash,
        "EFT" to R.string.payment_method_eft,
        "Card" to R.string.payment_method_card,
    )
    private val invoiceStatuses = mapOf(
        "Draft" to R.string.invoice_status_draft,
        "Sent" to R.string.invoice_status_sent,
        "Partially Paid" to R.string.invoice_status_partially_paid,
        "Paid" to R.string.invoice_status_paid,
        "Overdue" to R.string.invoice_status_overdue,
    )

    fun jobStatus(context: Context, code: String) = label(context, jobStatuses, code)
    fun jobStatusCode(context: Context, label: String) = code(context, jobStatuses, label)
    fun expenseCategory(context: Context, code: String) = label(context, expenseCategories, code)
    fun paymentMethod(context: Context, code: String) = label(context, paymentMethods, code)
    fun invoiceStatus(context: Context, code: String) = label(context, invoiceStatuses, code)
    fun invoiceStatusCode(context: Context, label: String) = code(context, invoiceStatuses, label)

    private fun label(context: Context, values: Map<String, Int>, code: String) =
        values[code]?.let(context::getString) ?: code

    private fun code(context: Context, values: Map<String, Int>, label: String) =
        values.entries.firstOrNull { context.getString(it.value) == label }?.key ?: label
}
