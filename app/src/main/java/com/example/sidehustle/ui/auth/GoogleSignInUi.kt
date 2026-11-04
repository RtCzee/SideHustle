package com.example.sidehustle.ui.auth

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.ui.navigation.resetSession
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/** Both entry screens use the same cancellation, loading and authenticated navigation rules. */
fun Fragment.launchGoogleSignIn(setLoading: (Boolean) -> Unit) {
    val root = view ?: return
    if (!isAdded || parentFragmentManager.isStateSaved || root.findViewById<android.view.View>(R.id.google_button)?.isEnabled != true) return
    val activity = requireActivity()
    val repository = (activity.application as SideHustleApp).repository
    setLoading(true)
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            when (val result = GoogleSignIn(activity, repository).signIn()) {
                is GoogleSignInResult.Success -> findNavController().resetSession(R.id.dashboardFragment)
                GoogleSignInResult.Cancelled -> view?.let {
                    Snackbar.make(it, R.string.google_sign_in_cancelled, Snackbar.LENGTH_SHORT).show()
                }
                is GoogleSignInResult.Failure -> view?.let {
                    val message = result.apiError?.message?.takeIf { message -> message.isNotBlank() }
                        ?: getString(result.message)
                    val snackbar = Snackbar.make(it, message, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.action_retry) { launchGoogleSignIn(setLoading) }
                    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) { snackbar.dismiss() }
                    })
                    snackbar.show()
                }
            }
        } finally { setLoading(false) }
    }
}
