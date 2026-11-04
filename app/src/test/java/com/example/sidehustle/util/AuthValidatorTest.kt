package com.example.sidehustle.util

import com.example.sidehustle.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** These exercise the validators called by the forms, with no Android runtime or mocked results. */
class AuthValidatorTest {
    @Test fun `account email is required but client email is optional`() {
        listOf("", " ", "\t\n").forEach { email ->
            assertEquals(R.string.error_email_required, AuthValidator.emailError(email))
            assertNull(AuthValidator.optionalEmailError(email))
        }
    }

    @Test fun `ordinary plus-addressed and subdomain emails are accepted`() {
        listOf("thabo@example.com", "lerato.dlamini+jobs@example.co.za",
            "USER@EXAMPLE.COM", "client@my-business.co.za", "a_b%tag@example.org").forEach { email ->
            assertNull(email, AuthValidator.emailError(email))
            assertNull(email, AuthValidator.optionalEmailError(email))
        }
    }

    @Test fun `malformed emails are rejected by both forms`() {
        listOf("user", "user@", "@example.com", "user@example", "user@@example.com",
            "user name@example.com", "user@example .com", " user@example.com",
            "user@example.com ", ".user@example.com", "user.@example.com",
            "user..name@example.com", "user@-example.com", "user@example-.com",
            "user@example..com", "user@example.c", "user@example.com\n").forEach { email ->
            assertEquals(email, R.string.error_email_invalid, AuthValidator.emailError(email))
            assertEquals(email, R.string.error_email_invalid, AuthValidator.optionalEmailError(email))
        }
    }

    @Test fun `email local-part and domain-label length boundaries are enforced`() {
        assertNull(AuthValidator.emailError("${"a".repeat(64)}@example.com"))
        assertEquals(R.string.error_email_invalid, AuthValidator.emailError("${"a".repeat(65)}@example.com"))
        assertNull(AuthValidator.emailError("user@${"a".repeat(63)}.com"))
        assertEquals(R.string.error_email_invalid, AuthValidator.emailError("user@${"a".repeat(64)}.com"))
        val prefix = "${"u".repeat(64)}@${"a".repeat(63)}.${"b".repeat(63)}."
        val longest = "$prefix${"c".repeat(57)}.com"
        assertEquals(254, longest.length)
        assertNull(AuthValidator.emailError(longest))
        assertEquals(R.string.error_email_invalid, AuthValidator.emailError("$prefix${"c".repeat(58)}.com"))
    }

    @Test fun `blank password reports required before minimum length`() {
        listOf("", " ", "      ", "\t\n").forEach {
            assertEquals(R.string.error_password_required, AuthValidator.passwordError(it))
        }
    }

    @Test fun `password minimum is exactly six characters`() {
        listOf("a", "12345").forEach {
            assertEquals(R.string.error_password_short, AuthValidator.passwordError(it))
        }
        assertNull(AuthValidator.passwordError("123456"))
        assertNull(AuthValidator.passwordError("longer password 123!"))
    }

    @Test fun `password spaces are preserved rather than trimmed`() {
        assertNull(AuthValidator.passwordError(" abcd "))
        assertEquals(R.string.error_password_mismatch, AuthValidator.confirmPasswordError(" abcd ", "abcd"))
    }

    @Test fun `confirmation must match exactly including case`() {
        assertNull(AuthValidator.confirmPasswordError("Secret1!", "Secret1!"))
        listOf("", "secret1!", "Secret1! ", "Different1!").forEach {
            assertEquals(R.string.error_password_mismatch, AuthValidator.confirmPasswordError("Secret1!", it))
        }
    }

    @Test fun `blank job titles are rejected and real titles accepted`() {
        listOf("", " ", "\n\t").forEach {
            assertEquals(R.string.error_title_required, AuthValidator.titleError(it))
        }
        assertNull(AuthValidator.titleError("Website for client"))
    }

    @Test fun `agreed amount accepts zero and finite positive amounts`() {
        listOf("0", "0.00", "0.01", "1250.75", " 25.50 ").forEach {
            assertNull(it, AuthValidator.amountError(it))
        }
    }

    @Test fun `agreed amount rejects negatives missing text and nonfinite numbers`() {
        listOf("", " ", "-0.01", "-100", "R 100", "1,000", "NaN", "Infinity", "-Infinity", "1e309").forEach {
            assertEquals(it, R.string.error_amount_invalid, AuthValidator.amountError(it))
        }
    }
}
