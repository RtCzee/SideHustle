package com.example.sidehustle.ui.common.error

import androidx.annotation.StringRes
import com.example.sidehustle.R
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.data.remote.ErrorKind

enum class ErrorAction(@get:StringRes val label: Int) {
    RETRY(R.string.action_retry), LOGIN(R.string.error_action_login),
    BACK(R.string.action_back), DISMISS(R.string.error_action_dismiss),
}

data class ErrorUiState(
    @get:StringRes val title: Int,
    @get:StringRes val message: Int,
    val action: ErrorAction,
    val validationMessage: String? = null,
)

/** HTTP status takes precedence over transport kind; unknown failures never expose raw messages. */
fun ApiResult.Error.toUiState(): ErrorUiState = when (httpCode) {
    401 -> ErrorUiState(R.string.error_session_title, R.string.error_session_message, ErrorAction.LOGIN)
    404 -> ErrorUiState(R.string.error_not_found_title, R.string.error_not_found_message, ErrorAction.BACK)
    400, 409, 422 -> ErrorUiState(
        R.string.error_validation_title, R.string.error_validation_message, ErrorAction.DISMISS,
        message.trim().takeIf { text ->
            text.isNotEmpty() && text.length <= 300 &&
                text.none { it == '<' || it == '>' || it == '{' || it == '}' || it.isISOControl() }
        },
    )
    in 500..599 -> ErrorUiState(R.string.error_server_title, R.string.error_server_message, ErrorAction.RETRY)
    else -> when (kind) {
        ErrorKind.NETWORK -> ErrorUiState(R.string.error_network_title, R.string.error_network_message, ErrorAction.RETRY)
        ErrorKind.TIMEOUT -> ErrorUiState(R.string.error_network_title, R.string.error_timeout_message, ErrorAction.RETRY)
        ErrorKind.UNKNOWN -> ErrorUiState(R.string.error_generic_title, R.string.error_generic_message, ErrorAction.RETRY)
    }
}
