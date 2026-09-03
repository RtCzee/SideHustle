package com.example.sidehustle.data.remote

import com.example.sidehustle.data.model.CreateProfileRequest
import com.example.sidehustle.data.model.HealthResponse
import com.example.sidehustle.data.model.UserProfileResponse
import com.example.sidehustle.network.ApiClient
import com.example.sidehustle.network.SideHustleApi
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RemoteDataSource(
    private val api: SideHustleApi = ApiClient.api,
) {

    suspend fun fetchHealth(): ApiResult<HealthResponse> = safeApiCall { api.getHealth() }

    suspend fun fetchProfile(): ApiResult<UserProfileResponse> = safeApiCall { api.getProfile() }

    suspend fun createProfile(request: CreateProfileRequest): ApiResult<UserProfileResponse> =
        safeApiCall { api.createProfile(request) }

    private suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (error: HttpException) {
            ApiResult.Error(mapHttpError(error.code()), error.code())
        } catch (_: SocketTimeoutException) {
            ApiResult.Error("The server took too long to respond. Try again.")
        } catch (_: UnknownHostException) {
            ApiResult.Error("No internet connection. Check your network and try again.")
        } catch (_: IOException) {
            ApiResult.Error("Could not reach the server. Check your connection and try again.")
        } catch (_: Exception) {
            ApiResult.Error("Something went wrong. Please try again.")
        }
    }

    private fun mapHttpError(code: Int): String {
        return when (code) {
            401 -> "Your session expired. Please log in again."
            404 -> "Profile not found."
            409 -> "A profile already exists for this account."
            in 500..599 -> "The server had a problem. Try again later."
            else -> "The request failed (HTTP $code)."
        }
    }
}
