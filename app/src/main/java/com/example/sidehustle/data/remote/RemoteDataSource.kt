package com.example.sidehustle.data.remote

import android.util.Log
import com.example.sidehustle.data.model.ClientRequest
import com.example.sidehustle.data.model.ClientResponse
import com.example.sidehustle.data.model.CreateProfileRequest
import com.example.sidehustle.data.model.CreateExpenseRequest
import com.example.sidehustle.data.model.DashboardResponse
import com.example.sidehustle.data.model.DeleteResult
import com.example.sidehustle.data.model.HealthResponse
import com.example.sidehustle.data.model.JobRequest
import com.example.sidehustle.data.model.JobResponse
import com.example.sidehustle.data.model.UpdateProfileRequest
import com.example.sidehustle.data.model.UserProfileResponse
import com.example.sidehustle.data.model.CreateInvoiceRequest
import com.example.sidehustle.data.model.InvoiceClient
import com.example.sidehustle.data.model.InvoiceJob
import com.example.sidehustle.data.model.InvoiceResponse
import com.example.sidehustle.data.model.UpdateInvoiceStatusRequest
import com.example.sidehustle.data.model.CreateIncomeRequest
import com.example.sidehustle.data.model.IncomeOptionsResponse
import com.example.sidehustle.data.model.IncomeResponse
import com.example.sidehustle.network.ApiClient
import com.example.sidehustle.network.SideHustleApi
import retrofit2.HttpException
import org.json.JSONObject
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

    suspend fun updateProfile(request: UpdateProfileRequest): ApiResult<UserProfileResponse> =
        safeApiCall { api.updateProfile(request) }

    suspend fun fetchDashboard(): ApiResult<DashboardResponse> = safeApiCall { api.getDashboard() }

    suspend fun fetchClients(): ApiResult<List<ClientResponse>> = safeApiCall { api.getClients() }

    suspend fun fetchClient(clientId: String): ApiResult<ClientResponse> =
        safeApiCall { api.getClient(clientId) }

    suspend fun createClient(request: ClientRequest): ApiResult<ClientResponse> =
        safeApiCall { api.createClient(request) }

    suspend fun updateClient(clientId: String, request: ClientRequest): ApiResult<ClientResponse> =
        safeApiCall { api.updateClient(clientId, request) }

    suspend fun deleteClient(clientId: String): ApiResult<DeleteResult> =
        safeApiCall { api.deleteClient(clientId) }

    suspend fun fetchJobs(): ApiResult<List<JobResponse>> = safeApiCall { api.getJobs() }

    suspend fun fetchJob(jobId: String): ApiResult<JobResponse> = safeApiCall { api.getJob(jobId) }

    suspend fun createJob(request: JobRequest): ApiResult<JobResponse> =
        safeApiCall { api.createJob(request) }

    suspend fun updateJob(jobId: String, request: JobRequest): ApiResult<JobResponse> =
        safeApiCall { api.updateJob(jobId, request) }

    private suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (error: HttpException) {
            val body = error.response()?.errorBody()?.string()
            Log.e("SideHustle", "API HTTP ${error.code()}: ${body?.take(200)}")
            val serverMessage = body?.let { extractErrorMessage(it) }
            ApiResult.Error(serverMessage ?: mapHttpError(error.code()), error.code())
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

    /** The backend returns validation failures as {"error": "..."} — prefer that specific
     *  message over a generic one when it's present and readable. */
    private fun extractErrorMessage(body: String): String? = try {
        org.json.JSONObject(body).optString("error").takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private fun mapHttpError(code: Int): String {
        return when (code) {
            400 -> "Some details weren't valid. Check the form and try again."
            401 -> "Your session expired. Please log in again."
            404 -> "Profile not found."
            409 -> "A profile already exists for this account."
            in 500..599 -> "The server had a problem. Try again later."
            else -> "The request failed (HTTP $code)."
        }
    }

    /** Uses the API's useful error text instead of assigning every 404 to a missing profile. */
    private fun apiErrorMessage(body: String?): String? = try {
        JSONObject(body.orEmpty()).optString("error").takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}
