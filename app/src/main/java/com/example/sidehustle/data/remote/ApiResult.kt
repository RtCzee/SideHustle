package com.example.sidehustle.data.remote

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()

    data class Error(
        val message: String,
        val httpCode: Int? = null,
        val kind: ErrorKind = ErrorKind.UNKNOWN,
    ) : ApiResult<Nothing>()
}

enum class ErrorKind { NETWORK, TIMEOUT, UNKNOWN }
