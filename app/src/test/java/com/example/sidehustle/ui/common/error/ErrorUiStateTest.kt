package com.example.sidehustle.ui.common.error

import com.example.sidehustle.R
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.data.remote.ErrorKind
import org.junit.Assert.*
import org.junit.Test

class ErrorUiStateTest {
    @Test fun `transport failures offer retry with distinct connection and timeout messages`() {
        val network = ApiResult.Error("internal detail", kind = ErrorKind.NETWORK).toUiState()
        val timeout = ApiResult.Error("internal detail", kind = ErrorKind.TIMEOUT).toUiState()
        assertEquals(ErrorAction.RETRY, network.action)
        assertEquals(R.string.error_network_message, network.message)
        assertEquals(R.string.error_timeout_message, timeout.message)
    }

    @Test fun `HTTP status takes precedence and recommends the correct recovery`() {
        val cases = mapOf(401 to ErrorAction.LOGIN, 404 to ErrorAction.BACK,
            400 to ErrorAction.DISMISS, 422 to ErrorAction.DISMISS,
            500 to ErrorAction.RETRY, 503 to ErrorAction.RETRY)
        cases.forEach { (code, action) ->
            assertEquals(action, ApiResult.Error("", code, ErrorKind.NETWORK).toUiState().action)
        }
    }

    @Test fun `server and unknown errors never expose internal messages`() {
        listOf<Int?>(null, 401, 404, 403, 500, 599).forEach { code ->
            val state = ApiResult.Error("SQL error with private data", code).toUiState()
            assertNull(state.validationMessage)
        }
        assertEquals(R.string.error_server_message, ApiResult.Error("", 500).toUiState().message)
        assertEquals(R.string.error_generic_message, ApiResult.Error("").toUiState().message)
    }

    @Test fun `readable validation survives while blank markup and oversized messages fall back`() {
        assertEquals("Name is required.", ApiResult.Error(" Name is required. ", 400).toUiState().validationMessage)
        listOf("", "  ", "<html>Proxy failed</html>", "{\"error\":true}", "a".repeat(301), "Bad\u0000input").forEach {
            assertNull(ApiResult.Error(it, 400).toUiState().validationMessage)
        }
    }
}
