package com.example.sidehustle.data.remote

import com.example.sidehustle.ui.common.error.ErrorAction
import com.example.sidehustle.ui.common.error.toUiState
import org.junit.Assert.*
import org.junit.Test

class ApiErrorMapperTest {
    @Test fun `validation JSON supplies the readable message shown by the UI`() {
        listOf(400, 409, 422).forEach { code ->
            val error = ApiErrorMapper.fromHttp(code, fixture("validation-error.json"))
            assertEquals(code, error.httpCode)
            assertEquals("Amount must be greater than zero.", error.message)
            assertEquals(error.message, error.toUiState().validationMessage)
            assertEquals(ErrorAction.DISMISS, error.toUiState().action)
        }
    }

    @Test fun `missing malformed and nonstring errors use a friendly fallback`() {
        listOf(null, "", fixture("malformed-error.txt"), "<html>Bad gateway</html>",
            "{}", "[]", "null", "{\"error\":null}", "{\"error\":123}",
            "{\"error\":true}", "{\"error\":{\"detail\":\"private\"}}", "{\"error\":\"  \"}")
            .forEach { body ->
                assertEquals(ApiResult.Error("Some details weren't valid. Check the form and try again.", 400),
                    ApiErrorMapper.fromHttp(400, body))
            }
    }

    @Test fun `server JSON cannot expose diagnostics and offers retry`() {
        val error = ApiErrorMapper.fromHttp(500, fixture("server-error.json"))
        assertEquals(ApiResult.Error("Something went wrong on our side. Please try again.", 500), error)
        assertEquals(ErrorAction.RETRY, error.toUiState().action)
        assertNull(error.toUiState().validationMessage)
    }

    @Test fun `auth and missing resource responses keep their safe messages and actions`() {
        val body = fixture("server-error.json")
        val auth = ApiErrorMapper.fromHttp(401, body)
        assertEquals("Session expired. Please log in again.", auth.message)
        assertEquals(ErrorAction.LOGIN, auth.toUiState().action)
        val missing = ApiErrorMapper.fromHttp(404, body)
        assertEquals("We could not find that item.", missing.message)
        assertEquals(ErrorAction.BACK, missing.toUiState().action)
    }

    private fun fixture(name: String) = checkNotNull(javaClass.getResource("/api/$name")).readText()
}
