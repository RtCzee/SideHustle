package com.example.sidehustle

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.ui.common.error.ErrorStateView
import com.example.sidehustle.ui.common.error.toUiState
import com.example.sidehustle.ui.common.error.showFullPageError
import com.example.sidehustle.ui.navigation.navigateSafely
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorStatesInstrumentedTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun fullPageRetryInvokesTheOperationAndUsesTheMappedAction() {
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_SideHustle)
            val view = ErrorStateView(context)
            var attempts = 0
            view.render(ApiResult.Error("private server detail", 500).toUiState(), { attempts++ })
            val action = view.findViewById<TextView>(R.id.error_action)
            assertEquals(context.getString(R.string.action_retry), action.text.toString())
            action.performClick()
            action.performClick()
            assertEquals(2, attempts)
            assertEquals(context.getString(R.string.error_server_message),
                view.findViewById<TextView>(R.id.error_message).text.toString())
            view.render(ApiResult.Error("", 401).toUiState(), {})
            assertEquals(context.getString(R.string.error_action_login), action.text.toString())
        }
    }

    @Test fun missingAndMalformedDeepLinkIdsShow404WithoutOpeningTheForm() {
        listOf("sidehustle://projects", "sidehustle://projects/invalid").forEach { uri ->
            ActivityScenario.launch<MainActivity>(Intent(Intent.ACTION_VIEW, Uri.parse(uri),
                instrumentation.targetContext, MainActivity::class.java)).use { scenario ->
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    assertEquals(activity.getString(R.string.error_not_found_message),
                        activity.findViewById<TextView>(R.id.error_message).text.toString())
                    assertEquals(View.GONE, activity.findViewById<View>(R.id.form_group).visibility)
                    assertEquals(activity.getString(R.string.action_back),
                        activity.findViewById<TextView>(R.id.error_action).text.toString())
                }
            }
        }
    }

    @Test fun unknownDestinationAndWrongArgumentTypeHaveAVisibleFallback() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sidehustle://unknown"),
            instrumentation.targetContext, MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val nav = host.navController
                assertEquals(R.id.notFoundFragment, nav.currentDestination?.id)
                nav.navigateSafely(Int.MAX_VALUE)
                assertEquals(R.id.notFoundFragment, nav.currentDestination?.id)
                nav.navigateSafely(R.id.projectDetailsFragment, bundleOf("jobId" to 123))
                assertEquals(R.id.notFoundFragment, nav.currentDestination?.id)
            }
        }
    }

    @Test fun expiredSessionActionClearsProtectedNavigation() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sidehustle://unknown"),
            instrumentation.targetContext, MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val fragment = host.childFragmentManager.primaryNavigationFragment!!
                fragment.showFullPageError(fragment.requireView() as ErrorStateView, ApiResult.Error("", 401)) {}
                activity.findViewById<View>(R.id.error_action).performClick()
                assertEquals(R.id.loginFragment, host.navController.currentDestination?.id)
                assertNull(host.navController.previousBackStackEntry)
            }
        }
    }
}
