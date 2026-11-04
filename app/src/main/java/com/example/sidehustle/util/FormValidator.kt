package com.example.sidehustle.util

import com.example.sidehustle.R
import java.math.BigDecimal
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

/** Pure checks shared by create/edit screens. Limits match NUMERIC(12,2) in the API schema. */
object FormValidator {
    private val decimal = Regex("(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)")
    private val maxAmount = BigDecimal("9999999999.99")

    fun moneyError(text: String, allowZero: Boolean = false): Int? {
        val invalid = if (allowZero) R.string.error_amount_invalid else R.string.error_amount_positive
        val raw = text.trim()
        if (!decimal.matches(raw)) return invalid
        val amount = raw.toBigDecimalOrNull() ?: return invalid
        if (amount.signum() < 0 || (!allowZero && amount.signum() == 0)) return invalid
        if (amount > maxAmount) return R.string.error_amount_too_large
        if (amount.stripTrailingZeros().scale() > 2) return R.string.error_amount_precision
        return null
    }

    fun dateError(text: String, required: Boolean = true): Int? {
        if (text.isBlank()) return if (required) R.string.error_date_required else null
        if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(text) || text.startsWith("0000")) return R.string.error_date_invalid
        val position = ParsePosition(0)
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(text, position)
        return if (date != null && position.index == text.length) null else R.string.error_date_invalid
    }

    fun selectionError(value: String?, allowed: Collection<String>, message: Int): Int? =
        if (value.isNullOrBlank() || value !in allowed) message else null

    fun clientError(id: String?, availableIds: Collection<String>): Int? =
        selectionError(id, availableIds, R.string.error_client_required)

    /** An invoice bills its chosen job; the total is read-only and must still be validated. */
    fun invoiceError(clientId: String?, jobId: String?, amount: Double?): Int? = when {
        clientId.isNullOrBlank() -> R.string.error_client_required
        jobId.isNullOrBlank() -> R.string.invoice_job_required
        amount == null || !amount.isFinite() -> R.string.invoice_amount_invalid
        else -> if (moneyError(BigDecimal.valueOf(amount).toPlainString(), allowZero = true) == null) null else R.string.invoice_amount_invalid
    }
}
