package com.example.sidehustle.util

import android.util.Patterns
import com.example.sidehustle.R

/**
 * Field checks for auth screens. Returns a string resource id, or null if the value is valid.
 */
object AuthValidator {

    fun nameError(name: String): Int? =
        if (name.isBlank()) R.string.error_name_required else null

    fun emailError(email: String): Int? = when {
        email.isBlank() -> R.string.error_email_required
        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> R.string.error_email_invalid
        else -> null
    }

    fun passwordError(password: String): Int? = when {
        password.isBlank() -> R.string.error_password_required
        password.length < 6 -> R.string.error_password_short
        else -> null
    }

    fun confirmPasswordError(password: String, confirm: String): Int? =
        if (password != confirm) R.string.error_password_mismatch else null

    /** Phone is optional, so blank is valid — but if something is entered, it must look like a number. */
    private val PHONE_PATTERN = Regex("^\\+?[0-9 ()-]{7,20}$")

    fun phoneError(phone: String): Int? =
        if (phone.isBlank() || PHONE_PATTERN.matches(phone)) null else R.string.error_phone_invalid

    /** Unlike account email (emailError), a client's email is optional — blank is valid. */
    fun optionalEmailError(email: String): Int? =
        if (email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            null
        } else {
            R.string.error_email_invalid
        }

    fun titleError(title: String): Int? =
        if (title.isBlank()) R.string.error_title_required else null

    /** Agreed amount must parse as a number and be >= 0 (issue: Build Job Management). */
    fun amountError(amount: String): Int? {
        val value = amount.trim().toDoubleOrNull() ?: return R.string.error_amount_invalid
        return if (value < 0 || value.isNaN() || value.isInfinite()) R.string.error_amount_invalid else null
    }
}
