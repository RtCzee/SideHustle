package com.example.sidehustle.ui.common.loading

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.example.sidehustle.R
import com.example.sidehustle.databinding.ViewLoadingOverlayBinding

/**
 * Blocking overlay for short, critical actions (e.g. creating an invoice): dims the screen and
 * swallows touches so the action cannot be repeated or interrupted by tapping around.
 *
 * Back button rules:
 *  - `show(message)` with no `onCancel`: Back is NOT intercepted. It behaves as it always does
 *    (the user can leave the screen).
 *  - `show(message, onCancel = { ... })`: a Cancel button appears and Back triggers the same
 *    cancel action. Use this when the action can be safely abandoned.
 *
 * ```
 * // Fragment.onViewCreated
 * overlay = BlockingLoadingOverlay(
 *     binding.overlay,                         // <include layout="@layout/view_loading_overlay" />
 *     viewLifecycleOwner,
 *     requireActivity().onBackPressedDispatcher,
 * )
 *
 * viewLifecycleOwner.lifecycleScope.launch {
 *     overlay.show(R.string.creating_invoice) { job?.cancel() } // your own string resource
 *     try { repository.createInvoice(...) } finally { overlay.hide() }
 * }
 * ```
 *
 * Note: the overlay covers the Fragment's area only; the bottom navigation bar stays tappable.
 * Leaving the screen cancels work started in `viewLifecycleOwner.lifecycleScope`.
 */
class BlockingLoadingOverlay(
    private val binding: ViewLoadingOverlayBinding,
    lifecycleOwner: LifecycleOwner,
    backDispatcher: OnBackPressedDispatcher,
) {

    private var onCancel: (() -> Unit)? = null

    // Only enabled while the overlay is showing AND has a cancel action.
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = cancel()
    }

    // Blocks touches straight away (no show delay) but keeps the overlay up briefly so it
    // does not flash when the action finishes almost instantly.
    private val handler = LoadingStateHandler.forViews(
        loadingView = binding.overlayRoot,
        contentView = null,
        lifecycleOwner = lifecycleOwner,
        showDelayMs = 0L,
        minShowMs = MIN_SHOW_MS,
    )

    init {
        // Registered against the view lifecycle, so it is removed when the view is destroyed.
        backDispatcher.addCallback(lifecycleOwner, backCallback)
        binding.overlayCancelButton.setOnClickListener { cancel() }
    }

    val isShowing: Boolean get() = handler.isLoading

    /**
     * @param messageRes what is happening, e.g. a "Creating invoice…" string
     * @param onCancel if not null, shows a Cancel button and lets Back cancel the action
     */
    fun show(@StringRes messageRes: Int = R.string.loading_please_wait, onCancel: (() -> Unit)? = null) {
        this.onCancel = onCancel
        binding.overlayMessage.setText(messageRes)
        binding.overlayCancelButton.isVisible = onCancel != null
        backCallback.isEnabled = onCancel != null
        handler.setLoading(true)
    }

    /** Safe to call at any time, including when the overlay is not showing. */
    fun hide() {
        onCancel = null
        // Stop intercepting Back immediately, even if the overlay lingers for its minimum time.
        backCallback.isEnabled = false
        handler.setLoading(false)
    }

    private fun cancel() {
        val action = onCancel
        onCancel = null
        backCallback.isEnabled = false
        handler.hideImmediately()
        action?.invoke()
    }

    private companion object {
        const val MIN_SHOW_MS = 300L
    }
}
