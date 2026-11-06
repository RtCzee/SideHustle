package com.example.sidehustle.ui.common.list

/** Empty is a successful result, never a substitute for a failed or pending request. */
class ListStateMachine {
    enum class Phase { LOADING, CONTENT, EMPTY, ERROR }
    var phase = Phase.LOADING
        private set
    var refreshing = false
        private set
    private var loaded = false
    private var empty = false

    fun begin() {
        refreshing = loaded && phase != Phase.ERROR
        if (!refreshing) phase = Phase.LOADING
    }

    fun success(isEmpty: Boolean) {
        loaded = true
        empty = isEmpty
        refreshing = false
        phase = if (empty) Phase.EMPTY else Phase.CONTENT
    }

    fun failure(requiresLogin: Boolean = false) {
        refreshing = false
        phase = if (!loaded || requiresLogin) Phase.ERROR
        else if (empty) Phase.EMPTY else Phase.CONTENT
    }
}
