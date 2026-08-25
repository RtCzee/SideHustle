package com.example.sidehustle

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.sidehustle.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

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

        binding.bottomNav.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.isVisible = destination.id in topLevelDestinations
        }

        if (savedInstanceState == null && auth.currentUser != null) {
            Log.d(TAG, "Existing session for ${auth.currentUser?.email}")
            navController.navigate(R.id.action_loginFragment_to_dashboardFragment)
        }
    }

    companion object {
        private const val TAG = "SideHustle"
    }
}
