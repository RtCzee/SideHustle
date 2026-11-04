package com.example.sidehustle

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val nav get() = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
    private var keyboardVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
        if (savedInstanceState == null && intent?.data != null) {
            val request = NavDeepLinkRequest.Builder.fromUri(intent.data!!).build()
            if (!nav.graph.hasDeepLink(request)) nav.showNotFound()
        }
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
        if (intent.data == null) return
        try {
            if (!nav.handleDeepLink(intent)) nav.showNotFound()
        } catch (_: IllegalArgumentException) {
            nav.showNotFound()
        }
    }
}