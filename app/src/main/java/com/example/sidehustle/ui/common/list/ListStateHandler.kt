package com.example.sidehustle.ui.common.list

import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.ui.common.error.dismissInlineError
import com.example.sidehustle.ui.common.error.showInlineError

/** Own this with the Fragment view; rows survive nonfatal refresh failures. */
class ListStateHandler(
    private val fragment: Fragment,
    private val stateView: ListStateView,
    private val content: View,
    @param:StringRes private val emptyTitle: Int,
    @param:StringRes private val emptyMessage: Int,
    @param:StringRes private val emptyAction: Int,
    private val onEmptyAction: () -> Unit,
    private val refreshIndicator: View? = null,
) {
    private val state = ListStateMachine()

    fun loading() {
        fragment.dismissInlineError()
        state.begin()
        refreshIndicator?.isVisible = state.refreshing
        if (state.phase == ListStateMachine.Phase.LOADING) {
            content.isVisible = false
            stateView.showLoading()
        }
    }

    fun success(isEmpty: Boolean) {
        state.success(isEmpty)
        renderLoaded()
    }

    fun error(error: ApiResult.Error, retry: () -> Unit) {
        state.failure(requiresLogin = error.httpCode == 401)
        refreshIndicator?.isVisible = false
        if (state.phase == ListStateMachine.Phase.ERROR) {
            content.isVisible = false
            stateView.showError(fragment, error, retry)
        } else {
            renderLoaded()
            fragment.showInlineError(error, retry)
        }
    }

    private fun renderLoaded() {
        refreshIndicator?.isVisible = false
        content.isVisible = state.phase == ListStateMachine.Phase.CONTENT
        if (state.phase == ListStateMachine.Phase.EMPTY) {
            stateView.showEmpty(emptyTitle, emptyMessage, emptyAction, onEmptyAction)
        } else stateView.isVisible = false
    }
}
