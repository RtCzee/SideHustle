package com.example.sidehustle.util

import java.text.NumberFormat

internal fun formatMoney(currency: String, amount: Double, useRandSymbol: Boolean = false): String {
    val prefix = if (useRandSymbol && currency == "ZAR") "R" else currency
    return "$prefix ${NumberFormat.getNumberInstance().format(amount)}"
}
