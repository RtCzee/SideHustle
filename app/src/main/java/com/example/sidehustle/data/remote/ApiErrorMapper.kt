package com.example.sidehustle.data.remote

import com.google.gson.JsonParser
import com.google.gson.JsonParseException

/** Pure JSON-to-error mapping, also used when the response body is absent or malformed. */
internal object ApiErrorMapper {
    fun fromHttp(code: Int, body: String?): ApiResult.Error {
        // Only input errors may expose a server message. Other bodies can contain internals.
        val message = if (code in listOf(400, 409, 422)) body?.let(::validationMessage) else null
        return ApiResult.Error(message ?: fallback(code), code)
    }

    private fun validationMessage(body: String): String? = try {
        val json = JsonParser.parseString(body)
        val error = if (json.isJsonObject) json.asJsonObject.get("error") else null
        if (error?.isJsonPrimitive == true && error.asJsonPrimitive.isString) {
            error.asString.takeIf { it.isNotBlank() }
        } else null
    } catch (_: JsonParseException) {
        null
    }

    private fun fallback(code: Int): String = when (code) {
        400 -> "Some details weren't valid. Check the form and try again."
        401 -> "Session expired. Please log in again."
        404 -> "We could not find that item."
        409 -> "A profile already exists for this account."
        in 500..599 -> "Something went wrong on our side. Please try again."
        else -> "Something went wrong. Please try again."
    }
}
