package com.example.sidehustle

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.NavHostFragment
import com.example.sidehustle.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // Screens where the floating curved nav should appear
    private val topLevelDestinations = setOf(
        R.id.dashboardFragment,
        R.id.clientsFragment,
        R.id.projectsFragment,
        R.id.invoicesFragment,
        R.id.settingsFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Custom glass nav — see ui/navigation/CurvedBottomNavBar.kt
        binding.floatingNav.setup(navController)
        applyNavInsets()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.floatingNav.isVisible = destination.id in topLevelDestinations
        }

        if (savedInstanceState == null && auth.currentUser != null) {
            Log.d(TAG, "Existing session for ${auth.currentUser?.email}")
            navController.navigate(R.id.action_loginFragment_to_dashboardFragment)
        }
    }

    /** Keeps the nav above the system gesture bar on all devices. */
    private fun applyNavInsets() {
        val baseBottomMargin = resources.getDimensionPixelSize(R.dimen.curved_nav_margin_bottom)

        ViewCompat.setOnApplyWindowInsetsListener(binding.floatingNav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<android.widget.FrameLayout.LayoutParams> {
                bottomMargin = systemBars.bottom + baseBottomMargin
            }
            insets
        }
    }

    companion object {
        private const val TAG = "SideHustle"
    }
}
