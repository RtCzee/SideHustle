package com.example.sidehustle.util

import com.example.sidehustle.R
import org.junit.Assert.*
import org.junit.Test

class FormValidatorTest {
    @Test fun `all money forms reject missing negative nonfinite and malformed amounts`() {
        listOf("", " ", "-1", "-0.01", "NaN", "Infinity", "1e309", "1e3", "1,000", "R 10", "1..2").forEach { value ->
            assertNotNull(value, FormValidator.moneyError(value))
            assertNotNull(value, AuthValidator.amountError(value))
        }
    }

    @Test fun `income and expenses require positive money while jobs can be free`() {
        listOf("0", "0.00", "000.000").forEach {
            assertEquals(R.string.error_amount_positive, FormValidator.moneyError(it))
            assertNull(AuthValidator.amountError(it))
        }
        listOf("0.01", ".50", "1", "25.50", " 1250.75 ", "9999999999.99").forEach {
            assertNull(it, FormValidator.moneyError(it))
            assertNull(it, AuthValidator.amountError(it))
        }
    }

    @Test fun `money fits the database limit and cannot silently round cents`() {
        listOf("10000000000", "9999999999.999", "9".repeat(100)).forEach {
            assertEquals(R.string.error_amount_too_large, FormValidator.moneyError(it))
        }
        listOf("0.001", "1.234", "12.345678").forEach {
            assertEquals(R.string.error_amount_precision, FormValidator.moneyError(it))
        }
        assertNull(FormValidator.moneyError("12.3400"))
    }

    @Test fun `required dates reject blanks and impossible calendar dates`() {
        assertEquals(R.string.error_date_required, FormValidator.dateError(""))
        listOf("2026-02-29", "2026-04-31", "2026-13-01", "0000-01-01", "2026-1-01",
            "01/11/2026", "2026-11-01T00:00:00Z", "2026-11-01junk").forEach {
            assertEquals(it, R.string.error_date_invalid, FormValidator.dateError(it))
        }
        listOf("2024-02-29", "2026-11-04", "2026-12-31").forEach {
            assertNull(it, FormValidator.dateError(it))
        }
    }

    @Test fun `job dates can be omitted but nonblank dates must still be real`() {
        assertNull(FormValidator.dateError("", required = false))
        assertNull(FormValidator.dateError(" ", required = false))
        assertNotNull(FormValidator.dateError("2026-02-30", required = false))
    }

    @Test fun `jobs require an available client rather than arbitrary text or a stale ID`() {
        listOf(null, "", " ", "deleted-client").forEach {
            assertEquals(R.string.error_client_required, FormValidator.clientError(it, listOf("client-1")))
        }
        assertNotNull(FormValidator.clientError("client-1", emptyList()))
        assertNull(FormValidator.clientError("client-1", listOf("client-1")))
    }

    @Test fun `invoices cannot be created without both a client and a job`() {
        assertEquals(R.string.error_client_required, FormValidator.invoiceError(null, "job-1", 100.0))
        assertEquals(R.string.error_client_required, FormValidator.invoiceError(" ", "job-1", 100.0))
        assertEquals(R.string.invoice_job_required, FormValidator.invoiceError("client-1", null, null))
        assertEquals(R.string.invoice_job_required, FormValidator.invoiceError("client-1", "", 100.0))
    }

    @Test fun `read only invoice totals reject missing negative nonfinite out of range and fractional cents`() {
        listOf(null, -0.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 10000000000.0, 1.234).forEach {
            assertEquals(it.toString(), R.string.invoice_amount_invalid, FormValidator.invoiceError("client-1", "job-1", it))
        }
        listOf(0.0, 0.01, 1250.75, 9999999999.99).forEach {
            assertNull(FormValidator.invoiceError("client-1", "job-1", it))
        }
    }

    @Test fun `dropdowns reject missing and unsupported values instead of defaulting silently`() {
        listOf(null, "", " ", "bitcoin").forEach {
            assertNotNull(FormValidator.selectionError(it, listOf("Cash", "EFT", "Card"), R.string.income_payment_method_error))
        }
        assertNull(FormValidator.selectionError("EFT", listOf("Cash", "EFT", "Card"), R.string.income_payment_method_error))
        assertNotNull(FormValidator.selectionError("invalid", listOf("ZAR", "USD"), R.string.error_currency_required))
        assertNotNull(FormValidator.selectionError("", listOf("English"), R.string.error_language_required))
        assertNotNull(FormValidator.selectionError("Done", listOf("Pending", "Completed"), R.string.error_status_required))
    }

    @Test fun `names and titles cannot overflow their database columns`() {
        assertNull(AuthValidator.nameError("a".repeat(255)))
        assertNotNull(AuthValidator.nameError("a".repeat(256)))
        assertNull(AuthValidator.titleError("a".repeat(255)))
        assertNotNull(AuthValidator.titleError("a".repeat(256)))
    }

    @Test fun `optional phones need real digits when supplied`() {
        listOf("-------", "() () ()", "+-------", "123 ( )", "1234567890123456").forEach {
            assertNotNull(it, AuthValidator.phoneError(it))
        }
        assertNull(AuthValidator.phoneError(""))
        assertNull(AuthValidator.phoneError("+27 (82) 123-4567"))
    }
}
