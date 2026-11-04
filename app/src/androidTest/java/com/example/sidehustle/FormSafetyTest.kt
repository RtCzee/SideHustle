package com.example.sidehustle

import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sidehustle.data.model.*
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.data.remote.ErrorKind
import com.example.sidehustle.data.repository.SideHustleRepository
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume

/** All saves and loads use a fake repository. Never writes account data or signs out Firebase. */
@RunWith(AndroidJUnit4::class)
class FormSafetyTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as SideHustleApp
    private lateinit var fake: OfflineRepository

    @Before fun installFakeRepository() {
        assumeNotNull(FirebaseAuth.getInstance().currentUser)
        fake = OfflineRepository()
        instrumentation.runOnMainSync { app.repositoryOverride = fake.repository }
    }

    @After fun restoreRepository() {
        instrumentation.runOnMainSync { app.repositoryOverride = null }
    }

    @Test fun clientsRejectMissingNameAndInvalidContactsThenKeepInputOnNetworkFailure() =
        screen(R.id.clientDetailsFragment, bundleOf("isCreating" to true)) { scenario ->
            scenario.root { root ->
                edit(root, R.id.name_input, " ")
                edit(root, R.id.email_input, "bad-email")
                edit(root, R.id.phone_input, "-------")
                root.findViewById<View>(R.id.save_button).performClick()
                fieldError(root, R.id.name_layout)
                fieldError(root, R.id.email_layout)
                fieldError(root, R.id.phone_layout)
                assertEquals(0, fake.writes)
                edit(root, R.id.name_input, "Demo client")
                edit(root, R.id.email_input, "")
                edit(root, R.id.phone_input, "")
                root.findViewById<View>(R.id.save_button).performClick()
                assertEquals("Demo client", root.findViewById<EditText>(R.id.name_input).text.toString())
                assertTrue(root.findViewById<View>(R.id.save_button).isEnabled)
                retryNetworkFailure(root)
                assertEquals(2, fake.writes)
            }
        }

    @Test fun editingAClientUsesTheSameRequiredAndOptionalFieldChecks() =
        screen(R.id.clientDetailsFragment, bundleOf("clientId" to CLIENT_ID)) { scenario ->
            scenario.root { root ->
                edit(root, R.id.name_input, "")
                root.findViewById<View>(R.id.save_button).performClick()
                fieldError(root, R.id.name_layout)
                assertEquals(0, fake.writes)
                edit(root, R.id.name_input, "Edited client")
                root.findViewById<View>(R.id.save_button).performClick()
                assertEquals("Edited client", root.findViewById<EditText>(R.id.name_input).text.toString())
                retryNetworkFailure(root)
            }
        }

    @Test fun jobsCannotBeCreatedWithoutAClientTitleOrAmount() =
        screen(R.id.projectDetailsFragment, bundleOf("isCreating" to true)) { scenario ->
            scenario.root { root ->
                root.findViewById<View>(R.id.save_button).performClick()
                fieldError(root, R.id.client_layout)
                fieldError(root, R.id.title_layout)
                fieldError(root, R.id.amount_layout)
                assertEquals(0, fake.writes)
                val client = root.findViewById<AutoCompleteTextView>(R.id.client_input)
                client.setText("Demo client", false)
                client.onItemClickListener!!.onItemClick(null, null, 0, 0)
                edit(root, R.id.title_input, "Demo project")
                edit(root, R.id.amount_input, "100")
                // Clearing the label must not keep a previously selected ID valid.
                client.setText("", false)
                root.findViewById<View>(R.id.save_button).performClick()
                fieldError(root, R.id.client_layout)
                assertEquals(0, fake.writes)
            }
        }

    @Test fun jobEditsRejectBadMoneyStatusAndDatesAndRetainInputAfterFailure() =
        screen(R.id.projectDetailsFragment, bundleOf("jobId" to JOB_ID)) { scenario ->
            scenario.root { root ->
                listOf("", "-5", "0.001", "10000000000").forEach { amount ->
                    edit(root, R.id.amount_input, amount)
                    root.findViewById<View>(R.id.save_button).performClick()
                    fieldError(root, R.id.amount_layout)
                }
                edit(root, R.id.amount_input, "100")
                edit(root, R.id.status_input, "Unknown")
                edit(root, R.id.due_date_input, "2026-02-30")
                root.findViewById<View>(R.id.save_button).performClick()
                fieldError(root, R.id.status_layout)
                fieldError(root, R.id.due_date_layout)
                assertEquals(0, fake.writes)
                edit(root, R.id.status_input, "Pending")
                edit(root, R.id.due_date_input, "")
                root.findViewById<View>(R.id.save_button).performClick()
                assertEquals("100", root.findViewById<EditText>(R.id.amount_input).text.toString())
                retryNetworkFailure(root)
            }
        }

    @Test fun incomeRejectsBadAmountsDatesAndMissingPaymentMethod() = screen(R.id.incomeFragment) { scenario ->
        scenario.root { it.findViewById<View>(R.id.toggle_form).performClick() }
        instrumentation.waitForIdleSync()
        scenario.root { root ->
            listOf("", "-5", "0", "0.001", "10000000000").forEach { amount ->
                edit(root, R.id.income_amount, amount)
                root.findViewById<View>(R.id.save_income).performClick()
                assertNotNull(root.findViewById<EditText>(R.id.income_amount).error)
            }
            assertNotNull(root.findViewById<EditText>(R.id.income_date).error)
            assertTrue(root.findViewById<TextView>(R.id.income_status).isShown)
            assertEquals(0, fake.writes)
            edit(root, R.id.income_amount, "12.50")
            edit(root, R.id.income_date, "2026-02-30")
            root.findViewById<Spinner>(R.id.payment_method).setSelection(1)
            root.findViewById<View>(R.id.save_income).performClick()
            assertNotNull(root.findViewById<EditText>(R.id.income_date).error)
            edit(root, R.id.income_date, "2026-11-04")
            root.findViewById<View>(R.id.save_income).performClick()
            assertEquals("12.50", root.findViewById<EditText>(R.id.income_amount).text.toString())
            assertTrue(root.findViewById<View>(R.id.save_income).isEnabled)
            retryNetworkFailure(root)
        }
    }

    @Test fun expensesRejectBadAmountsAndMissingCategoryAndPreventDuplicatePendingSaves() = screen(R.id.expensesFragment) { scenario ->
        scenario.root { root ->
            root.findViewById<View>(R.id.toggle_form).performClick()
            listOf("", "-5", "0", "0.001", "10000000000").forEach { amount ->
                edit(root, R.id.amount_input, amount)
                root.findViewById<View>(R.id.save_expense_button).performClick()
                assertNotNull(root.findViewById<EditText>(R.id.amount_input).error)
            }
            assertNotNull(root.findViewById<EditText>(R.id.date_input).error)
            assertTrue(root.findViewById<TextView>(R.id.expenses_status).isShown)
            assertEquals(0, fake.writes)
            edit(root, R.id.amount_input, "45.25")
            edit(root, R.id.date_input, "2026-11-04")
            root.findViewById<Spinner>(R.id.category_input).setSelection(1)
            fake.holdWrite = true
            root.findViewById<View>(R.id.save_expense_button).performClick()
            root.findViewById<View>(R.id.save_expense_button).performClick()
            assertEquals(1, fake.writes)
            assertFalse(root.findViewById<View>(R.id.save_expense_button).isEnabled)
            fake.holdWrite = false
            fake.pending!!.resume(fake.networkError)
            assertEquals("45.25", root.findViewById<EditText>(R.id.amount_input).text.toString())
            assertTrue(root.findViewById<View>(R.id.save_expense_button).isEnabled)
            retryNetworkFailure(root)
            assertEquals(2, fake.writes)
        }
    }

    @Test fun invoicesRequireClientAndJobAndRejectANegativeDerivedAmount() = screen(R.id.invoicesFragment) { scenario ->
        scenario.root { it.findViewById<View>(R.id.toggle_form).performClick() }
        instrumentation.waitForIdleSync()
        scenario.root { root ->
            root.findViewById<View>(R.id.create_invoice).performClick()
            assertEquals(app.getString(R.string.error_client_required), root.findViewById<TextView>(R.id.invoice_message).text.toString())
            fake.invoiceAmount = -5.0
            root.findViewById<Spinner>(R.id.invoice_client).setSelection(1)
        }
        instrumentation.waitForIdleSync()
        scenario.root { root ->
            root.findViewById<View>(R.id.create_invoice).performClick()
            assertEquals(app.getString(R.string.invoice_job_required), root.findViewById<TextView>(R.id.invoice_message).text.toString())
            root.findViewById<Spinner>(R.id.invoice_job).setSelection(1)
        }
        instrumentation.waitForIdleSync()
        scenario.root { root ->
            root.findViewById<View>(R.id.create_invoice).performClick()
            assertEquals(app.getString(R.string.invoice_amount_invalid), root.findViewById<TextView>(R.id.invoice_message).text.toString())
            assertEquals(0, fake.writes)
        }
    }

    @Test fun invoiceNetworkFailureKeepsSelectionsAndOffersRetry() = screen(R.id.invoicesFragment) { scenario ->
        scenario.root { it.findViewById<View>(R.id.toggle_form).performClick() }
        instrumentation.waitForIdleSync()
        scenario.root { it.findViewById<Spinner>(R.id.invoice_client).setSelection(1) }
        instrumentation.waitForIdleSync()
        scenario.root { it.findViewById<Spinner>(R.id.invoice_job).setSelection(1) }
        instrumentation.waitForIdleSync()
        scenario.root { root ->
            root.findViewById<View>(R.id.create_invoice).performClick()
            assertEquals(1, root.findViewById<Spinner>(R.id.invoice_client).selectedItemPosition)
            assertEquals(1, root.findViewById<Spinner>(R.id.invoice_job).selectedItemPosition)
            assertTrue(root.findViewById<View>(R.id.create_invoice).isEnabled)
            retryNetworkFailure(root)
            assertEquals(2, fake.writes)
        }
    }

    @Test fun settingsValidateRequiredFieldsAndKeepEditsAfterNetworkFailure() = screen(R.id.settingsFragment) { scenario ->
        scenario.root { root ->
            val currency = root.findViewById<TextView>(R.id.currency_input).text.toString()
            val language = root.findViewById<TextView>(R.id.language_input).text.toString()
            edit(root, R.id.name_input, "")
            edit(root, R.id.phone_input, "-------")
            edit(root, R.id.currency_input, "")
            edit(root, R.id.language_input, "Unknown")
            root.findViewById<View>(R.id.save_button).performClick()
            listOf(R.id.name_layout, R.id.phone_layout, R.id.currency_layout, R.id.language_layout).forEach { fieldError(root, it) }
            assertEquals(0, fake.writes)
            edit(root, R.id.name_input, "Demo profile")
            edit(root, R.id.phone_input, "")
            edit(root, R.id.currency_input, currency)
            edit(root, R.id.language_input, language)
            root.findViewById<View>(R.id.save_button).performClick()
            assertEquals("Demo profile", root.findViewById<EditText>(R.id.name_input).text.toString())
            retryNetworkFailure(root)
        }
    }

    @Test fun settingsInitialNetworkFailureShowsRetryAndCanRecover() {
        fake.failProfile = true
        screen(R.id.settingsFragment) { scenario ->
            scenario.root { root ->
                assertEquals(View.VISIBLE, root.findViewById<View>(R.id.error_state).visibility)
                assertEquals(View.GONE, root.findViewById<View>(R.id.form_group).visibility)
                fake.failProfile = false
                root.findViewById<View>(R.id.error_action).performClick()
                assertEquals(View.VISIBLE, root.findViewById<View>(R.id.form_group).visibility)
            }
        }
    }

    private fun screen(destination: Int, args: Bundle? = null, check: (ActivityScenario<MainActivity>) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val nav = (activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
                nav.navigate(destination, args, NavOptions.Builder().setPopUpTo(R.id.dashboardFragment, false).build())
            }
            instrumentation.waitForIdleSync()
            check(scenario)
        }
    }

    private fun ActivityScenario<MainActivity>.root(check: (View) -> Unit) = onActivity { activity ->
        val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        check(host.childFragmentManager.primaryNavigationFragment!!.requireView())
    }

    private fun edit(root: View, id: Int, text: String) {
        val input = root.findViewById<EditText>(id)
        if (input is AutoCompleteTextView) input.setText(text, false) else input.setText(text)
    }

    private fun fieldError(root: View, id: Int) = assertFalse(root.findViewById<TextInputLayout>(id).error.isNullOrBlank())

    private fun retryNetworkFailure(root: View) {
        val snackbar = root.getTag(R.id.error_snackbar) as Snackbar
        assertEquals(app.getString(R.string.error_network_message), snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).text.toString())
        snackbar.view.findViewById<View>(com.google.android.material.R.id.snackbar_action).performClick()
    }

    private class OfflineRepository {
        var writes = 0
        var failProfile = false
        var invoiceAmount = 100.0
        var holdWrite = false
        var pending: Continuation<Any?>? = null
        val networkError = ApiResult.Error("Offline", kind = ErrorKind.NETWORK)
        private val client = ClientResponse(CLIENT_ID, "Demo client")
        @Suppress("UNCHECKED_CAST")
        val repository = Proxy.newProxyInstance(SideHustleRepository::class.java.classLoader,
            arrayOf(SideHustleRepository::class.java)) { _, method, args ->
            when {
                method.name.startsWith("create") || method.name.startsWith("update") || method.name.startsWith("delete") -> {
                    writes++
                    if (holdWrite) {
                        pending = args!!.last() as Continuation<Any?>
                        COROUTINE_SUSPENDED
                    } else networkError
                }
                method.name == "fetchDashboard" -> ApiResult.Success(DashboardResponse("Demo profile", "ZAR", 0.0, 0.0, 0.0, 0.0, 0, 0))
                method.name == "fetchProfile" -> if (failProfile) networkError else ApiResult.Success(UserProfileResponse("demo", "demo@example.com", "Demo profile", preferredCurrency = "ZAR", preferredLanguage = "en"))
                method.name == "fetchClients" -> ApiResult.Success(listOf(client))
                method.name == "fetchClient" -> ApiResult.Success(client)
                method.name == "fetchJobs" -> ApiResult.Success(emptyList<JobResponse>())
                method.name == "fetchJob" -> ApiResult.Success(JobResponse(JOB_ID, CLIENT_ID, "Demo client", "Demo project", status = "Pending", agreedAmount = 100.0, currency = "ZAR"))
                method.name == "fetchInvoiceClients" -> ApiResult.Success(listOf(InvoiceClient(CLIENT_ID, "Demo client")))
                method.name == "fetchInvoiceJobs" -> ApiResult.Success(listOf(InvoiceJob(JOB_ID, "Demo project", invoiceAmount, "ZAR")))
                method.name == "fetchInvoices" -> ApiResult.Success(emptyList<InvoiceResponse>())
                method.name == "fetchIncome" -> ApiResult.Success(emptyList<IncomeResponse>())
                method.name == "fetchExpenses" -> ApiResult.Success(emptyList<ExpenseResponse>())
                method.name == "fetchIncomeOptions" -> ApiResult.Success(IncomeOptionsResponse(emptyList(), emptyList(), emptyList()))
                else -> error("Unexpected repository call: ${method.name}")
            }
        } as SideHustleRepository
    }

    companion object {
        private const val CLIENT_ID = "11111111-1111-4111-8111-111111111111"
        private const val JOB_ID = "22222222-2222-4222-8222-222222222222"
    }
}
