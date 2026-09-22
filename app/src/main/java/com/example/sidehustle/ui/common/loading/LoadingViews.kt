package com.example.sidehustle.ui.common.loading

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/** [LoadingScheduler] backed by the main looper. */
class MainThreadLoadingScheduler : LoadingScheduler {
    private val handler = Handler(Looper.getMainLooper())

    override fun nowMs(): Long = SystemClock.uptimeMillis()

    override fun postDelayed(task: Runnable, delayMs: Long) {
        handler.postDelayed(task, delayMs)
    }

    override fun cancel(task: Runnable) {
        handler.removeCallbacks(task)
    }
}

/**
 * Wire a [LoadingStateHandler] to real views.
 *
 * @param loadingView the included loading layout (view_loading, view_list_loading, ...). It should
 *   start hidden; the handler shows and hides it.
 * @param contentView the content the loading view replaces. It is hidden while loading is showing.
 *   Pass null for something that sits on top of content (an overlay).
 * @param lifecycleOwner pass `viewLifecycleOwner` so pending callbacks are cancelled when the
 *   Fragment's view is destroyed (this is what makes late API responses safe).
 */
fun LoadingStateHandler.Companion.forViews(
    loadingView: View,
    contentView: View? = null,
    lifecycleOwner: LifecycleOwner? = null,
    showDelayMs: Long = LoadingStateHandler.DEFAULT_SHOW_DELAY_MS,
    minShowMs: Long = LoadingStateHandler.DEFAULT_MIN_SHOW_MS,
): LoadingStateHandler {
    val handler = LoadingStateHandler(
        scheduler = MainThreadLoadingScheduler(),
        onShow = {
            loadingView.isVisible = true
            contentView?.isVisible = false
        },
        onHide = {
            loadingView.isVisible = false
            contentView?.isVisible = true
        },
        showDelayMs = showDelayMs,
        minShowMs = minShowMs,
    )
    lifecycleOwner?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            handler.dispose()
            owner.lifecycle.removeObserver(this)
        }
    })
    return handler
}
