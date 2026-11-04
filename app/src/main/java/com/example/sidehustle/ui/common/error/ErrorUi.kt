package com.example.sidehustle.ui.common.error

import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.ui.navigation.navigateSafely
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

fun Fragment.showFullPageError(view: ErrorStateView, error: ApiResult.Error, retry: () -> Unit) {
    val state = error.toUiState()
    view.render(
        state,
        onAction = { performErrorAction(state.action, retry) },
        onBack = if (state.action == ErrorAction.RETRY) ({ goBackFromError() }) else null,
    )
}

/** Keep existing content and form input on refresh/save failure. Dismiss with the view lifecycle. */
fun Fragment.showInlineError(error: ApiResult.Error, retry: () -> Unit) {
    val root = view ?: return
    dismissInlineError()
    val state = error.toUiState()
    val snackbar = Snackbar.make(
        root, state.validationMessage ?: getString(state.message), Snackbar.LENGTH_INDEFINITE,
    ).setAction(state.action.label) { performErrorAction(state.action, retry, inline = true) }
    snackbar.setBackgroundTint(MaterialColors.getColor(root, com.google.android.material.R.attr.colorSurface))
    snackbar.setTextColor(MaterialColors.getColor(root, com.google.android.material.R.attr.colorOnSurface))
    snackbar.setActionTextColor(MaterialColors.getColor(root, com.google.android.material.R.attr.colorOnSurface))
    activity?.findViewById<View>(R.id.floating_nav)?.takeIf { it.isVisible }?.let { snackbar.setAnchorView(it) }
    val owner = viewLifecycleOwner
    root.setTag(R.id.error_snackbar, snackbar)
    val observer = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            snackbar.dismiss()
            root.setTag(R.id.error_snackbar, null)
            owner.lifecycle.removeObserver(this)
        }
    }
    owner.lifecycle.addObserver(observer)
    snackbar.addCallback(object : Snackbar.Callback() {
        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            owner.lifecycle.removeObserver(observer)
            if (root.getTag(R.id.error_snackbar) === snackbar) root.setTag(R.id.error_snackbar, null)
        }
    })
    snackbar.show()
}

fun Fragment.dismissInlineError() {
    (view?.getTag(R.id.error_snackbar) as? Snackbar)?.dismiss()
}

private fun Fragment.performErrorAction(action: ErrorAction, retry: () -> Unit, inline: Boolean = false) {
    if (view == null || parentFragmentManager.isStateSaved) return
    when (action) {
        ErrorAction.RETRY -> retry()
        ErrorAction.LOGIN -> {
            FirebaseAuth.getInstance().signOut()
            val nav = findNavController()
            nav.navigateSafely(R.id.loginFragment, options = NavOptions.Builder()
                .setPopUpTo(nav.graph.id, true).setLaunchSingleTop(true).build())
        }
        ErrorAction.BACK -> goBackFromError()
        ErrorAction.DISMISS -> if (!inline) goBackFromError()
    }
}

fun Fragment.goBackFromError() {
    val nav = findNavController()
    if (!nav.popBackStack()) {
        val destination = if (FirebaseAuth.getInstance().currentUser == null) R.id.loginFragment else R.id.dashboardFragment
        nav.navigateSafely(destination, options = NavOptions.Builder()
            .setPopUpTo(nav.graph.id, true).setLaunchSingleTop(true).build())
    }
}
