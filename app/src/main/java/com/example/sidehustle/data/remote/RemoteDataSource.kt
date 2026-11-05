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
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

import com.example.sidehustle.data.model.ExpenseResponse

class RemoteDataSource(
    private val api: SideHustleApi = ApiClient.api,
) {

    suspend fun sync(request: com.example.sidehustle.data.model.SyncRequest): ApiResult<com.example.sidehustle.data.model.SyncResponse> = safeApiCall { api.sync(request) }

    suspend fun registerPushDevice(request: com.example.sidehustle.data.model.PushDeviceRequest) =
        safeApiCall { api.registerPushDevice(request) }

    suspend fun fetchAttachments(type: String, id: String) = safeApiCall { api.getAttachments(type, id) }

    suspend fun addAttachment(type: String, id: String, request: com.example.sidehustle.data.model.AttachmentRequest) =
        safeApiCall { api.addAttachment(type, id, request) }

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

    suspend fun fetchExpenses(): ApiResult<List<ExpenseResponse>> = safeApiCall { api.getExpenses() }

    suspend fun createExpense(request: CreateExpenseRequest): ApiResult<ExpenseResponse> =
        safeApiCall { api.createExpense(request) }

    suspend fun fetchIncomeOptions(): ApiResult<IncomeOptionsResponse> =
        safeApiCall { api.getIncomeOptions() }

    suspend fun fetchIncome(): ApiResult<List<IncomeResponse>> = safeApiCall { api.getIncome() }

    suspend fun createIncome(request: CreateIncomeRequest): ApiResult<IncomeResponse> =
        safeApiCall { api.createIncome(request) }

    suspend fun fetchInvoiceClients(): ApiResult<List<InvoiceClient>> =
        safeApiCall { api.getInvoiceClients() }

    suspend fun fetchInvoiceJobs(clientId: String): ApiResult<List<InvoiceJob>> =
        safeApiCall { api.getInvoiceJobs(clientId) }

    suspend fun fetchInvoices(): ApiResult<List<InvoiceResponse>> = safeApiCall { api.getInvoices() }

    suspend fun createInvoice(request: CreateInvoiceRequest): ApiResult<InvoiceResponse> =
        safeApiCall { api.createInvoice(request) }

    suspend fun updateInvoiceStatus(invoiceId: String, status: String): ApiResult<InvoiceResponse> =
        safeApiCall { api.updateInvoiceStatus(invoiceId, UpdateInvoiceStatusRequest(status)) }

    internal suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
        val result = try {
            ApiResult.Success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: HttpException) {
            val body = try { error.response()?.errorBody()?.string() } catch (_: IOException) { null }
            Log.e("SideHustle", "API HTTP ${error.code()}")
            ApiErrorMapper.fromHttp(error.code(), body)
        } catch (_: SocketTimeoutException) {
            ApiResult.Error("The server took too long to respond. Try again.", kind = ErrorKind.TIMEOUT)
        } catch (_: UnknownHostException) {
            ApiResult.Error("Could not reach the server. Check your connection and try again.", kind = ErrorKind.NETWORK)
        } catch (_: IOException) {
            ApiResult.Error("Could not reach the server. Check your connection and try again.", kind = ErrorKind.NETWORK)
        } catch (_: Exception) {
            ApiResult.Error("Something went wrong. Please try again.")
        }
        // Retrofit can deliver an HTTP/IO failure as the view's request is cancelled.
        // Never turn that late result into UI work for a destroyed Fragment view.
        currentCoroutineContext().ensureActive()
        return result
    }

}
