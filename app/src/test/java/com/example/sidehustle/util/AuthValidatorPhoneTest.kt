package com.example.sidehustle.util

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers AuthValidator.phoneError and nameError, shared by the Settings and Client
 * details save flows. emailError/optionalEmailError/passwordError aren't tested here
 * since they call android.util.Patterns, which needs an Android runtime
 * (Robolectric/instrumented test), not a plain JVM test.
 * Covers AuthValidator.phoneError, used by the Settings screen save flow.
 * (nameError is covered indirectly here too since Settings reuses it for the name field.)
 */
class AuthValidatorPhoneTest {

    @Test
    fun `blank phone is valid since phone is optional`() {
        assertNull(AuthValidator.phoneError(""))
        assertNull(AuthValidator.phoneError("   "))
    }

    @Test
    fun `plausible phone numbers are valid`() {
        assertNull(AuthValidator.phoneError("+27821234567"))
        assertNull(AuthValidator.phoneError("082 123 4567"))
        assertNull(AuthValidator.phoneError("(082) 123-4567"))
        assertNull(AuthValidator.phoneError("0821234567"))
    }

    @Test
    fun `letters or too-short input is rejected`() {
        assertNotNull(AuthValidator.phoneError("call me maybe"))
        assertNotNull(AuthValidator.phoneError("12345"))
        assertNotNull(AuthValidator.phoneError("082-abc-4567"))
    }

    @Test
    fun `blank name is rejected`() {
        assertNotNull(AuthValidator.nameError(""))
        assertNotNull(AuthValidator.nameError("   "))
    }

    @Test
    fun `non-blank name is valid`() {
        assertNull(AuthValidator.nameError("Thabo Mokoena"))
    }
}
