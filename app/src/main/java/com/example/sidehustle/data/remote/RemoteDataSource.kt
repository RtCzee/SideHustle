package com.example.sidehustle.data.remote

import android.util.Log
import com.example.sidehustle.data.model.CreateProfileRequest
import com.example.sidehustle.data.model.DashboardResponse
import com.example.sidehustle.data.model.HealthResponse
import com.example.sidehustle.data.model.UserProfileResponse
import com.example.sidehustle.data.model.InvoiceClient
import com.example.sidehustle.data.model.InvoiceJob
import com.example.sidehustle.data.model.InvoiceResponse
import com.example.sidehustle.data.model.CreateInvoiceRequest
import com.example.sidehustle.data.model.UpdateInvoiceStatusRequest
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

    suspend fun fetchDashboard(): ApiResult<DashboardResponse> = safeApiCall { api.getDashboard() }
    suspend fun fetchInvoiceClients(): ApiResult<List<InvoiceClient>> = safeApiCall { api.getInvoiceClients() }
    suspend fun fetchInvoiceJobs(clientId: String): ApiResult<List<InvoiceJob>> = safeApiCall { api.getInvoiceJobs(clientId) }
    suspend fun fetchInvoices(): ApiResult<List<InvoiceResponse>> = safeApiCall { api.getInvoices() }
    suspend fun createInvoice(request: CreateInvoiceRequest): ApiResult<InvoiceResponse> = safeApiCall { api.createInvoice(request) }
    suspend fun updateInvoiceStatus(id: String, status: String): ApiResult<InvoiceResponse> = safeApiCall { api.updateInvoiceStatus(id, UpdateInvoiceStatusRequest(status)) }

    private suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (error: HttpException) {
            val body = error.response()?.errorBody()?.string()?.take(120)
            Log.e("SideHustle", "API HTTP ${error.code()}: $body")
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
