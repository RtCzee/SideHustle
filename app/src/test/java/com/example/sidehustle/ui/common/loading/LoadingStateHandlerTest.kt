package com.example.sidehustle.ui.common.loading

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

class LoadingStateHandlerTest {

    private val scheduler = FakeScheduler()
    private var shows = 0
    private var hides = 0

    private fun handler(showDelayMs: Long = 150, minShowMs: Long = 350) = LoadingStateHandler(
        scheduler = scheduler,
        onShow = { shows++ },
        onHide = { hides++ },
        showDelayMs = showDelayMs,
        minShowMs = minShowMs,
    )

    @Test
    fun `fast response never shows the loading UI`() {
        val loading = handler()
        loading.setLoading(true)
        scheduler.advance(100)
        loading.setLoading(false)
        scheduler.advance(2_000)

        assertEquals(0, shows)
        assertEquals(0, hides)
        assertFalse(loading.isShowing)
    }

    @Test
    fun `show delay restarts when loading is requested again`() {
        val loading = handler()
        loading.setLoading(true) // t=0, would show at 150
        scheduler.advance(100)
        loading.setLoading(false) // finished before the delay: nothing shown
        scheduler.advance(20)
        loading.setLoading(true) // t=120, a NEW request: should show at 270, not 150
        scheduler.advance(50) // t=170
        assertFalse("must wait the full delay for the new request", loading.isShowing)
        scheduler.advance(100) // t=270
        assertTrue(loading.isShowing)
        assertEquals(1, shows)
    }

    @Test
    fun `slow response shows after the delay and hides when finished`() {
        val loading = handler()
        loading.setLoading(true)
        scheduler.advance(149)
        assertFalse(loading.isShowing)
        scheduler.advance(1)
        assertTrue(loading.isShowing)

        scheduler.advance(5_000) // well past the minimum show time
        loading.setLoading(false)

        assertFalse(loading.isShowing)
        assertEquals(1, shows)
        assertEquals(1, hides)
    }

    @Test
    fun `hide waits out the minimum show time`() {
        val loading = handler()
        loading.setLoading(true)
        scheduler.advance(150) // shown at t=150
        scheduler.advance(100) // t=250
        loading.setLoading(false)

        assertTrue(loading.isShowing)
        scheduler.advance(249) // t=499, one ms before 150 + 350
        assertTrue(loading.isShowing)
        scheduler.advance(1)
        assertFalse(loading.isShowing)
        assertEquals(1, hides)
    }

    @Test
    fun `loading requested again while a hide is pending keeps the UI up without flicker`() {
        val loading = handler()
        loading.setLoading(true)
        scheduler.advance(150)
        scheduler.advance(50)
        loading.setLoading(false) // hide pending
        scheduler.advance(100)
        loading.setLoading(true) // ...but another request started
        scheduler.advance(10_000)

        assertTrue(loading.isShowing)
        assertEquals(1, shows)
        assertEquals(0, hides)
    }

    @Test
    fun `repeated identical calls are harmless`() {
        val loading = handler()
        repeat(5) { loading.setLoading(true) }
        scheduler.advance(150)
        assertEquals(1, shows)
        repeat(5) { loading.setLoading(false) }
        scheduler.advance(1_000)
        assertEquals(1, hides)
        assertEquals(0, scheduler.pendingCount)
    }

    @Test
    fun `rapid toggling always ends in the last requested state`() {
        val loading = handler()
        var t = 0
        repeat(200) { i ->
            loading.setLoading(i % 2 == 0)
            scheduler.advance((t++ % 7) * 20L)
        }
        loading.setLoading(false)
        scheduler.advance(5_000)

        assertFalse(loading.isLoading)
        assertFalse(loading.isShowing)
        assertEquals("every show is matched by a hide", shows, hides)
        assertEquals(0, scheduler.pendingCount)

        loading.setLoading(true)
        scheduler.advance(150)
        assertTrue(loading.isShowing)
    }

    @Test
    fun `dispose cancels a pending show`() {
        val loading = handler()
        loading.setLoading(true)
        loading.dispose()
        scheduler.advance(5_000)

        assertEquals(0, shows)
        assertEquals(0, scheduler.pendingCount)
    }

    @Test
    fun `dispose cancels a pending hide so views are not touched after destruction`() {
        val loading = handler()
        loading.setLoading(true)
        scheduler.advance(150)
        loading.setLoading(false) // hide pending
        loading.dispose()
        scheduler.advance(5_000)

        assertEquals(0, hides)
        assertEquals(0, scheduler.pendingCount)
    }

    @Test
    fun `calls after dispose are ignored`() {
        val loading = handler(showDelayMs = 0)
        loading.dispose()
        loading.setLoading(true)
        loading.hideImmediately()
        scheduler.advance(5_000)

        assertEquals(0, shows)
        assertEquals(0, hides)
        assertFalse(loading.isLoading)
    }

    @Test
    fun `zero delay shows immediately`() {
        val loading = handler(showDelayMs = 0)
        loading.setLoading(true)

        assertTrue(loading.isShowing)
        assertEquals(1, shows)
    }

    @Test
    fun `hideImmediately ignores the minimum show time`() {
        val loading = handler(showDelayMs = 0)
        loading.setLoading(true)
        loading.hideImmediately()

        assertFalse(loading.isShowing)
        assertFalse(loading.isLoading)
        assertEquals(1, hides)
        scheduler.advance(5_000)
        assertEquals(1, hides)
    }

    @Test
    fun `whileLoading turns loading off when the block throws`() {
        val loading = handler(showDelayMs = 0, minShowMs = 0)

        val result = runSuspending<Unit> {
            loading.whileLoading { error("network exploded") }
        }

        assertTrue(result.isFailure)
        assertFalse(loading.isLoading)
        assertFalse(loading.isShowing)
    }

    @Test
    fun `whileLoading returns the block result`() {
        val loading = handler(showDelayMs = 0, minShowMs = 0)

        val result = runSuspending { loading.whileLoading { 42 } }

        assertEquals(42, result.getOrNull())
        assertFalse(loading.isLoading)
    }

    @Test
    fun `overlapping whileLoading calls keep loading on until the last one finishes`() {
        val loading = handler(showDelayMs = 0, minShowMs = 0)
        val gateA = Gate()
        val gateB = Gate()

        runSuspending<Unit> { loading.whileLoading { gateA.await() } }
        runSuspending<Unit> { loading.whileLoading { gateB.await() } }
        assertTrue(loading.isLoading)

        gateA.open()
        assertTrue("second request is still running", loading.isLoading)
        assertTrue(loading.isShowing)

        gateB.open()
        assertFalse(loading.isLoading)
        assertFalse(loading.isShowing)
    }

    // --- test helpers -------------------------------------------------------------------------

    private class FakeScheduler : LoadingScheduler {
        private class Entry(val at: Long, val task: Runnable)

        private val queue = mutableListOf<Entry>()
        private var now = 0L

        val pendingCount: Int get() = queue.size

        override fun nowMs(): Long = now

        override fun postDelayed(task: Runnable, delayMs: Long) {
            queue.add(Entry(now + delayMs, task))
        }

        override fun cancel(task: Runnable) {
            queue.removeAll { it.task === task }
        }

        /** Move time forward, running due tasks in order (tasks may schedule more tasks). */
        fun advance(ms: Long) {
            val target = now + ms
            while (true) {
                val next = queue.filter { it.at <= target }.minByOrNull { it.at } ?: break
                queue.remove(next)
                now = maxOf(now, next.at)
                next.task.run()
            }
            now = target
        }
    }

    private class Gate {
        private var continuation: Continuation<Unit>? = null

        suspend fun await() = suspendCoroutine<Unit> { continuation = it }

        fun open() {
            continuation!!.resume(Unit)
        }
    }

    /** Starts [block] as a coroutine. Completes synchronously unless the block suspends on a [Gate]. */
    private fun <T> runSuspending(block: suspend () -> T): Result<T> {
        var outcome: Result<T>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { outcome = it })
        return outcome ?: Result.failure(IllegalStateException("suspended"))
    }
}
