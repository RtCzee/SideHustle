package com.example.sidehustle.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.example.sidehustle.R

val mainDestinations = setOf(R.id.dashboardFragment, R.id.clientsFragment, R.id.projectsFragment,
    R.id.invoicesFragment, R.id.expensesFragment, R.id.incomeFragment, R.id.settingsFragment)
private val authDestinations = setOf(R.id.loginFragment, R.id.registerFragment)

fun requiredSessionDestination(destination: Int, signedIn: Boolean): Int? = when {
    signedIn && destination in authDestinations -> R.id.dashboardFragment
    !signedIn && (destination in mainDestinations || destination == R.id.clientDetailsFragment ||
        destination == R.id.projectDetailsFragment) -> R.id.loginFragment
    else -> null
}

/** Clear both active entries and bottom-tab saved stacks across login/logout boundaries. */
fun NavController.resetSession(destination: Int) {
    (mainDestinations + authDestinations + setOf(R.id.clientDetailsFragment, R.id.projectDetailsFragment))
        .forEach { clearBackStack(it) }
    navigateSafely(destination, options = NavOptions.Builder().setPopUpTo(graph.id, true)
        .setLaunchSingleTop(true).build())
}
