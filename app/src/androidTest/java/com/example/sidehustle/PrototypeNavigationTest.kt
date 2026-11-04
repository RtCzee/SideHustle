package com.example.sidehustle

import android.os.Bundle
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import androidx.navigation.fragment.NavHostFragment
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sidehustle.ui.common.list.ListStateView
import com.example.sidehustle.ui.navigation.CurvedBottomNavBar
import com.example.sidehustle.ui.navigation.resetSession
import com.google.firebase.auth.FirebaseAuth
import org.junit.Assume.assumeNotNull
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Uses an isolated navigation graph: never changes the phone's Firebase session or API data. */
@RunWith(AndroidJUnit4::class)
class PrototypeNavigationTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun mainTabsSurviveLeavingAnInFlightRequest() {
        assumeNotNull(FirebaseAuth.getInstance().currentUser)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            repeat(3) {
                listOf(R.id.clientsFragment, R.id.projectsFragment, R.id.invoicesFragment,
                    R.id.expensesFragment, R.id.incomeFragment, R.id.settingsFragment,
                    R.id.dashboardFragment).forEach { destination ->
                    scenario.onActivity { activity ->
                        val nav = (activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
                        nav.navigate(destination, null, NavOptions.Builder()
                            .setPopUpTo(R.id.dashboardFragment, false).setLaunchSingleTop(true).build())
                        assertEquals(destination, nav.currentDestination!!.id)
                    }
                    instrumentation.waitForIdleSync()
                }
            }
        }
    }

    @Test fun sessionResetClearsAuthEntriesAndSavedPrivateTabState() = instrumentation.runOnMainSync {
        val nav = NavHostController(instrumentation.targetContext)
        val store = ViewModelStore()
        nav.setViewModelStore(store)
        val navigator = TestNavigator()
        nav.navigatorProvider.addNavigator(navigator)
        val graph = NavGraph(NavGraphNavigator(nav.navigatorProvider)).apply {
            id = R.id.nav_graph
            listOf(R.id.loginFragment, R.id.registerFragment, R.id.dashboardFragment,
                R.id.clientsFragment, R.id.projectsFragment).forEach { destinationId ->
                addDestination(navigator.createDestination().apply { id = destinationId })
            }
            setStartDestination(R.id.loginFragment)
        }
        nav.graph = graph
        nav.navigate(R.id.registerFragment)
        nav.resetSession(R.id.dashboardFragment)
        assertNull(nav.previousBackStackEntry)
        nav.navigate(R.id.clientsFragment)
        nav.currentBackStackEntry!!.savedStateHandle["private"] = "old session"
        nav.navigate(R.id.projectsFragment, null,
            NavOptions.Builder().setPopUpTo(R.id.dashboardFragment, false, true).build())
        nav.resetSession(R.id.loginFragment)
        assertEquals(R.id.loginFragment, nav.currentDestination!!.id)
        assertNull(nav.previousBackStackEntry)
        nav.resetSession(R.id.dashboardFragment)
        nav.navigate(R.id.clientsFragment, null, NavOptions.Builder().setRestoreState(true).build())
        assertNull(nav.currentBackStackEntry!!.savedStateHandle.get<String>("private"))
        store.clear()
    }

    @Test fun allFiveLabelsFitAPhoneWithEnlargedText() = instrumentation.runOnMainSync {
        val configuration = Configuration(instrumentation.targetContext.resources.configuration).apply {
            fontScale = 1.3f
        }
        val context = ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(configuration), R.style.Theme_SideHustle)
        val density = context.resources.displayMetrics.density
        val nav = CurvedBottomNavBar(context)
        val width = (336 * density).toInt()
        val height = (106 * density).toInt()
        // Selecting the initial tab changes its typeface after the first layout.
        // Complete the follow-up layout that a real attached view receives.
        repeat(2) {
            nav.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            nav.layout(0, 0, width, height)
        }
        // Detached TextViews can defer creating their text layout until drawing.
        val rendered = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        nav.draw(Canvas(rendered))
        rendered.recycle()
        listOf(R.id.nav_label_dashboard, R.id.nav_label_clients, R.id.nav_label_projects,
            R.id.nav_label_finances, R.id.nav_label_settings).forEach { id ->
            val label = nav.findViewById<TextView>(id)
            assertTrue(label.width > 0)
            assertTrue(label.text.isNotBlank())
            val layout = label.layout
            assertEquals(0, layout.getEllipsisCount(layout.lineCount - 1))
        }
    }

    @Test fun sharedListStateNeverShowsEmptyWhileLoading() = instrumentation.runOnMainSync {
        val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_SideHustle)
        val state = ListStateView(context)
        state.showLoading()
        assertEquals(View.VISIBLE, state.findViewById<View>(R.id.loading_group).visibility)
        assertEquals(View.GONE, state.findViewById<View>(R.id.empty_group).visibility)
        var opened = false
        state.showEmpty(R.string.clients_empty_title, R.string.clients_empty_subtitle, R.string.action_add_client) { opened = true }
        assertEquals(View.GONE, state.findViewById<View>(R.id.loading_group).visibility)
        state.findViewById<View>(R.id.empty_action).performClick()
        assertTrue(opened)
    }

    @Navigator.Name("prototype-test")
    private class TestNavigator : Navigator<NavDestination>() {
        override fun createDestination() = NavDestination(this)
        override fun navigate(destination: NavDestination, args: Bundle?, navOptions: NavOptions?, navigatorExtras: Navigator.Extras?) = destination
        override fun popBackStack() = true
    }
}
