package com.example.sidehustle.ui.common.list

import org.junit.Assert.*
import org.junit.Test

class ListStateMachineTest {
    @Test fun `initial request cannot display an empty result before success`() {
        val state = ListStateMachine()
        state.begin()
        assertEquals(ListStateMachine.Phase.LOADING, state.phase)
        state.failure()
        assertEquals(ListStateMachine.Phase.ERROR, state.phase)
        state.begin()
        assertEquals(ListStateMachine.Phase.LOADING, state.phase)
        state.success(true)
        assertEquals(ListStateMachine.Phase.EMPTY, state.phase)
    }

    @Test fun `refresh errors preserve loaded content and stop the indicator`() {
        val state = ListStateMachine()
        state.success(false)
        state.begin()
        assertEquals(ListStateMachine.Phase.CONTENT, state.phase)
        assertTrue(state.refreshing)
        state.failure()
        assertEquals(ListStateMachine.Phase.CONTENT, state.phase)
        assertFalse(state.refreshing)
    }

    @Test fun `refreshing a known empty list retains its empty state`() {
        val state = ListStateMachine()
        state.success(true)
        state.begin()
        assertTrue(state.refreshing)
        state.failure()
        assertEquals(ListStateMachine.Phase.EMPTY, state.phase)
        state.success(false)
        assertEquals(ListStateMachine.Phase.CONTENT, state.phase)
    }

    @Test fun `expired sessions replace old rows and retries recover`() {
        val state = ListStateMachine()
        state.success(false)
        state.failure(requiresLogin = true)
        assertEquals(ListStateMachine.Phase.ERROR, state.phase)
        state.begin()
        assertEquals(ListStateMachine.Phase.LOADING, state.phase)
        state.success(false)
        assertEquals(ListStateMachine.Phase.CONTENT, state.phase)
        assertFalse(state.refreshing)
    }
}
