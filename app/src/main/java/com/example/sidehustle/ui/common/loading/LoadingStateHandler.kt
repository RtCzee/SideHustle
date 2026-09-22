package com.example.sidehustle.ui.common.loading

/**
 * Decides WHEN a loading UI should appear and disappear, so fast responses don't flicker.
 *
 * - The loading UI only appears if loading lasts longer than [showDelayMs].
 * - Once it appears it stays for at least [minShowMs], so it never blinks on and off.
 * - Toggling loading on/off rapidly is safe: pending show/hide callbacks are cancelled and
 *   the end state always matches the last call.
 *
 * This class has no Android dependencies (time comes from a [LoadingScheduler]) so it is unit
 * tested on the JVM. On a Fragment, create it with [forViews] from LoadingViews.kt.
 *
 * Threading: call everything from the main thread.
 *
 * @param onShow make the loading UI visible (and hide the content it replaces)
 * @param onHide hide the loading UI (and bring the content back)
 */
class LoadingStateHandler(
    private val scheduler: LoadingScheduler,
    private val onShow: () -> Unit,
    private val onHide: () -> Unit,
    private val showDelayMs: Long = DEFAULT_SHOW_DELAY_MS,
    private val minShowMs: Long = DEFAULT_MIN_SHOW_MS,
) {

    private var requested = false
    private var visible = false
    private var showPending = false
    private var disposed = false
    private var shownAtMs = 0L
    private var inFlight = 0

    private val showTask = Runnable {
        showPending = false
        if (!disposed && requested && !visible) showNow()
    }

    private val hideTask = Runnable {
        if (!disposed && !requested && visible) hideNow()
    }

    /** True from the moment loading was requested until it is turned off (even before it is visible). */
    val isLoading: Boolean get() = requested

    /** True while the loading UI is actually on screen. */
    val isShowing: Boolean get() = visible

    /** Request or end loading. Calling with the same value twice is harmless. */
    fun setLoading(loading: Boolean) {
        if (disposed) return
        if (loading) requestShow() else requestHide()
    }

    /**
     * Run [block] with loading on and turn it off afterwards, even if [block] throws or the
     * coroutine is cancelled. Overlapping calls are counted, so loading stays on until the
     * last one finishes.
     *
     * ```
     * val result = loading.whileLoading { repository.fetchProfile() }
     * ```
     */
    suspend fun <T> whileLoading(block: suspend () -> T): T {
        inFlight++
        setLoading(true)
        try {
            return block()
        } finally {
            inFlight--
            if (inFlight == 0) setLoading(false)
        }
    }

    /** Hide right now, ignoring the minimum show time (e.g. the user tapped Cancel). */
    fun hideImmediately() {
        if (disposed) return
        requested = false
        showPending = false
        scheduler.cancel(showTask)
        scheduler.cancel(hideTask)
        if (visible) hideNow()
    }

    /** Cancel pending callbacks. Call when the view is destroyed; the handler is unusable afterwards. */
    fun dispose() {
        disposed = true
        requested = false
        showPending = false
        scheduler.cancel(showTask)
        scheduler.cancel(hideTask)
    }

    private fun requestShow() {
        requested = true
        // A hide may be waiting out the minimum show time; loading came back, so keep showing.
        scheduler.cancel(hideTask)
        if (visible || showPending) return
        if (showDelayMs <= 0L) {
            showNow()
        } else {
            showPending = true
            scheduler.postDelayed(showTask, showDelayMs)
        }
    }

    private fun requestHide() {
        requested = false
        if (showPending) {
            // Finished before the delay elapsed: the loading UI never appears at all.
            showPending = false
            scheduler.cancel(showTask)
        }
        if (!visible) return
        val remaining = minShowMs - (scheduler.nowMs() - shownAtMs)
        scheduler.cancel(hideTask)
        if (remaining <= 0L) hideNow() else scheduler.postDelayed(hideTask, remaining)
    }

    private fun showNow() {
        visible = true
        shownAtMs = scheduler.nowMs()
        onShow()
    }

    private fun hideNow() {
        visible = false
        onHide()
    }

    companion object {
        const val DEFAULT_SHOW_DELAY_MS = 150L
        const val DEFAULT_MIN_SHOW_MS = 350L
    }
}

/** Time source + delayed execution for [LoadingStateHandler]. The real one wraps a main-thread Handler. */
interface LoadingScheduler {
    fun nowMs(): Long
    fun postDelayed(task: Runnable, delayMs: Long)
    fun cancel(task: Runnable)
}
