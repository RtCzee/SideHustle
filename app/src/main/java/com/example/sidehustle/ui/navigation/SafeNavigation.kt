package com.example.sidehustle.ui.navigation

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.example.sidehustle.R

/** Stale actions, unknown destinations and invalid arguments all have a visible fallback. */
fun NavController.navigateSafely(destination: Int, args: Bundle? = null, options: NavOptions? = null) {
    try {
        navigate(destination, args, options)
    } catch (_: IllegalArgumentException) {
        showNotFound()
    }
}

fun NavController.showNotFound() {
    if (currentDestination?.id != R.id.notFoundFragment) {
        navigate(R.id.notFoundFragment, null, NavOptions.Builder().setLaunchSingleTop(true).build())
    }
}
