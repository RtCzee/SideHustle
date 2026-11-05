package com.example.sidehustle

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.NavHostFragment
import com.example.sidehustle.databinding.ActivityMainBinding
import com.example.sidehustle.ui.navigation.isValidDetailRequest
import com.example.sidehustle.ui.navigation.mainDestinations
import com.example.sidehustle.ui.navigation.requiredSessionDestination
import com.example.sidehustle.ui.navigation.resetSession
import com.example.sidehustle.ui.navigation.showNotFound
import com.example.sidehustle.ui.navigation.navigateSafely
import com.example.sidehustle.notifications.ReminderMessagingService
import com.example.sidehustle.notifications.ReminderTarget
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val nav get() = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
    private var keyboardVisible = false
    private var pendingReminder: ReminderTarget? = null
    private var pushSyncJob: Job? = null
    private val pushRegistration get() = (application as SideHustleApp).pushRegistration
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pushRegistration.syncAsync()
        if (!granted) Snackbar.make(binding.root, R.string.reminder_permission_denied, Snackbar.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingReminder = ReminderTarget.from("job_due", savedInstanceState?.getString("reminder_uid"),
            savedInstanceState?.getString("reminder_job"))
        if (savedInstanceState == null) readReminderIntent(intent)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycleScope.launch {
            (application as SideHustleApp).syncStatus.collect { status ->
                binding.syncStatusText.isVisible = status != SyncStatus.ONLINE
                binding.syncStatusText.text = when (status) {
                    SyncStatus.OFFLINE -> getString(R.string.sync_offline)
                    SyncStatus.SYNCING -> getString(R.string.sync_syncing)
                    SyncStatus.ONLINE -> ""
                }
            }
        }
        binding.floatingNav.setup(nav)
        nav.addOnDestinationChangedListener { controller, destination, args ->
            // Missing IDs are public 404 states; valid resource requests require a session.
            val invalidDetail = when (destination.id) {
                R.id.projectDetailsFragment -> !isValidDetailRequest(args?.getString("jobId"), args?.getBoolean("isCreating") == true)
                R.id.clientDetailsFragment -> !isValidDetailRequest(args?.getString("clientId"), args?.getBoolean("isCreating") == true)
                else -> false
            }
            val redirect = if (invalidDetail) null else requiredSessionDestination(destination.id, auth.currentUser != null)
            if (redirect != null) controller.resetSession(redirect) else updateChrome()
            if (destination.id == R.id.dashboardFragment) syncPushRegistration()
            binding.root.post { openPendingReminder() }
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right,
                bottom = max(bars.bottom, keyboard.bottom))
            updateChrome()
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
        val deepLinkUri = intent?.data
        if (savedInstanceState == null && deepLinkUri != null) {
            val request = NavDeepLinkRequest.Builder.fromUri(deepLinkUri).build()
            if (!nav.graph.hasDeepLink(request)) nav.showNotFound()
        }
        binding.root.post { openPendingReminder() }
    }

    override fun onResume() {
        super.onResume()
        syncPushRegistration()
        openPendingReminder()
    }

    /** Ask once after sign-in; enabling notifications explicitly in Settings can ask again. */
    fun syncPushRegistration(requestPermission: Boolean = false) {
        if (auth.currentUser == null || pushSyncJob?.isActive == true) return
        pushSyncJob = lifecycleScope.launch {
            val enabled = pushRegistration.sync()
            if (enabled && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                (requestPermission || !pushRegistration.permissionWasRequested())) {
                pushRegistration.markPermissionRequested()
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun readReminderIntent(intent: Intent?): Boolean {
        val target = ReminderTarget.from("job_due", intent?.getStringExtra(ReminderMessagingService.EXTRA_USER_ID),
            intent?.getStringExtra(ReminderMessagingService.EXTRA_JOB_ID)) ?: return false
        pendingReminder = target
        return true
    }

    private fun openPendingReminder() {
        val target = pendingReminder ?: return
        val user = auth.currentUser ?: return // Keep the target until login completes.
        if (supportFragmentManager.isStateSaved) return
        pendingReminder = null
        if (user.uid == target.userId) {
            nav.navigateSafely(R.id.projectDetailsFragment, bundleOf("jobId" to target.jobId),
                androidx.navigation.NavOptions.Builder().setLaunchSingleTop(true).build())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingReminder?.let {
            outState.putString("reminder_uid", it.userId)
            outState.putString("reminder_job", it.jobId)
        }
        super.onSaveInstanceState(outState)
    }

    private fun updateChrome() {
        val showNav = nav.currentDestination?.id in mainDestinations && auth.currentUser != null && !keyboardVisible
        binding.floatingNav.isVisible = showNav
        val extraLabelSpace = ((resources.configuration.fontScale - 1f).coerceAtLeast(0f) * 40 * resources.displayMetrics.density).roundToInt()
        val navHeight = resources.getDimensionPixelSize(R.dimen.curved_nav_total_height) + extraLabelSpace
        val bottomMargin = resources.getDimensionPixelSize(R.dimen.curved_nav_margin_bottom)
        binding.floatingNav.updateLayoutParams<android.widget.FrameLayout.LayoutParams> {
            height = navHeight
            this.bottomMargin = bottomMargin
        }
        // Content ends above the complete nav, so its last row/actions are always reachable.
        binding.navHostFragment.updateLayoutParams<android.widget.FrameLayout.LayoutParams> {
            this.bottomMargin = if (showNav) navHeight + bottomMargin else 0
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (readReminderIntent(intent)) {
            openPendingReminder()
            return
        }
        if (intent.data == null) return
        try {
            if (!nav.handleDeepLink(intent)) nav.showNotFound()
        } catch (_: IllegalArgumentException) {
            nav.showNotFound()
        }
    }
}
