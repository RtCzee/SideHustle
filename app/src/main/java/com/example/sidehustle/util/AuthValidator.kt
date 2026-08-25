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
}
