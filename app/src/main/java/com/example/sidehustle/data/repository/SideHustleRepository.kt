package com.example.sidehustle.data.repository

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
import com.example.sidehustle.data.model.CreateIncomeRequest
import com.example.sidehustle.data.model.IncomeOptionsResponse
import com.example.sidehustle.data.model.IncomeResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.data.remote.RemoteDataSource
import com.example.sidehustle.data.local.LocalDataSource
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

import com.example.sidehustle.data.model.ExpenseResponse

interface SideHustleRepository {
    suspend fun fetchHealth(): ApiResult<HealthResponse>
    suspend fun fetchProfile(): ApiResult<UserProfileResponse>
    suspend fun createProfile(request: CreateProfileRequest): ApiResult<UserProfileResponse>
    suspend fun updateProfile(request: UpdateProfileRequest): ApiResult<UserProfileResponse>
    suspend fun fetchDashboard(): ApiResult<DashboardResponse>
    suspend fun fetchClients(): ApiResult<List<ClientResponse>>
    suspend fun fetchClient(clientId: String): ApiResult<ClientResponse>
    suspend fun createClient(request: ClientRequest): ApiResult<ClientResponse>
    suspend fun updateClient(clientId: String, request: ClientRequest): ApiResult<ClientResponse>
    suspend fun deleteClient(clientId: String): ApiResult<DeleteResult>
    suspend fun fetchJobs(): ApiResult<List<JobResponse>>
    suspend fun fetchJob(jobId: String): ApiResult<JobResponse>
    suspend fun createJob(request: JobRequest): ApiResult<JobResponse>
    suspend fun updateJob(jobId: String, request: JobRequest): ApiResult<JobResponse>
    suspend fun fetchExpenses(): ApiResult<List<ExpenseResponse>>
    suspend fun createExpense(request: CreateExpenseRequest): ApiResult<ExpenseResponse>
    suspend fun fetchIncomeOptions(): ApiResult<IncomeOptionsResponse>
    suspend fun fetchIncome(): ApiResult<List<IncomeResponse>>
    suspend fun createIncome(request: CreateIncomeRequest): ApiResult<IncomeResponse>
    suspend fun fetchInvoiceClients(): ApiResult<List<InvoiceClient>>
    suspend fun fetchInvoiceJobs(clientId: String): ApiResult<List<InvoiceJob>>
    suspend fun fetchInvoices(): ApiResult<List<InvoiceResponse>>
    suspend fun createInvoice(request: CreateInvoiceRequest): ApiResult<InvoiceResponse>
    suspend fun updateInvoiceStatus(invoiceId: String, status: String): ApiResult<InvoiceResponse>
}

class SideHustleRepositoryImpl(
    private val remote: RemoteDataSource = RemoteDataSource(),
    private val local: LocalDataSource? = null,
) : SideHustleRepository {

    private fun userId() = FirebaseAuth.getInstance().currentUser?.uid
    private fun isOffline(error: ApiResult.Error) = error.kind == com.example.sidehustle.data.remote.ErrorKind.NETWORK || error.kind == com.example.sidehustle.data.remote.ErrorKind.TIMEOUT

    override suspend fun fetchHealth(): ApiResult<HealthResponse> = remote.fetchHealth()

    override suspend fun fetchProfile(): ApiResult<UserProfileResponse> = remote.fetchProfile()

    override suspend fun createProfile(request: CreateProfileRequest): ApiResult<UserProfileResponse> =
        remote.createProfile(request)

    override suspend fun updateProfile(request: UpdateProfileRequest): ApiResult<UserProfileResponse> =
        remote.updateProfile(request)

    override suspend fun fetchDashboard(): ApiResult<DashboardResponse> = remote.fetchDashboard()

    override suspend fun fetchClients(): ApiResult<List<ClientResponse>> = cached("clients", remote.fetchClients()) { it.clientId }

    override suspend fun fetchClient(clientId: String): ApiResult<ClientResponse> = when (val r = remote.fetchClient(clientId)) {
        is ApiResult.Success -> { userId()?.let { local?.save(it, "clients", clientId, r.data) }; r }
        is ApiResult.Error -> if (isOffline(r)) local?.read<ClientResponse>(userId().orEmpty(), "clients", clientId)?.let { ApiResult.Success(it) } ?: r else r
    }

    override suspend fun createClient(request: ClientRequest): ApiResult<ClientResponse> =
        write("clients", UUID.randomUUID().toString(), request, remote.createClient(request)) { it.clientId }

    override suspend fun updateClient(clientId: String, request: ClientRequest): ApiResult<ClientResponse> =
        write("clients", clientId, request, remote.updateClient(clientId, request)) { it.clientId }

    override suspend fun deleteClient(clientId: String): ApiResult<DeleteResult> =
        remote.deleteClient(clientId)

    override suspend fun fetchJobs(): ApiResult<List<JobResponse>> = cached("jobs", remote.fetchJobs()) { it.jobId }

    override suspend fun fetchJob(jobId: String): ApiResult<JobResponse> = when (val r = remote.fetchJob(jobId)) {
        is ApiResult.Success -> { userId()?.let { local?.save(it, "jobs", jobId, r.data) }; r }
        is ApiResult.Error -> if (isOffline(r)) local?.read<JobResponse>(userId().orEmpty(), "jobs", jobId)?.let { ApiResult.Success(it) } ?: r else r
    }

    override suspend fun createJob(request: JobRequest): ApiResult<JobResponse> =
        write("jobs", UUID.randomUUID().toString(), request, remote.createJob(request)) { it.jobId }

    override suspend fun updateJob(jobId: String, request: JobRequest): ApiResult<JobResponse> =
        write("jobs", jobId, request, remote.updateJob(jobId, request)) { it.jobId }

    override suspend fun fetchExpenses(): ApiResult<List<ExpenseResponse>> = cached("expenses", remote.fetchExpenses()) { it.expenseId }

    override suspend fun createExpense(request: CreateExpenseRequest): ApiResult<ExpenseResponse> =
        write("expenses", UUID.randomUUID().toString(), request, remote.createExpense(request)) { it.expenseId }

    override suspend fun fetchIncomeOptions(): ApiResult<IncomeOptionsResponse> =
        remote.fetchIncomeOptions()

    override suspend fun fetchIncome(): ApiResult<List<IncomeResponse>> = cached("income", remote.fetchIncome()) { it.id }

    override suspend fun createIncome(request: CreateIncomeRequest): ApiResult<IncomeResponse> =
        write("income", UUID.randomUUID().toString(), request, remote.createIncome(request)) { it.id }

    override suspend fun fetchInvoiceClients(): ApiResult<List<InvoiceClient>> =
        remote.fetchInvoiceClients()

    override suspend fun fetchInvoiceJobs(clientId: String): ApiResult<List<InvoiceJob>> =
        remote.fetchInvoiceJobs(clientId)

    override suspend fun fetchInvoices(): ApiResult<List<InvoiceResponse>> = remote.fetchInvoices()

    override suspend fun createInvoice(request: CreateInvoiceRequest): ApiResult<InvoiceResponse> =
        remote.createInvoice(request)

    override suspend fun updateInvoiceStatus(invoiceId: String, status: String): ApiResult<InvoiceResponse> =
        remote.updateInvoiceStatus(invoiceId, status)

    private suspend inline fun <reified T> cached(kind: String, result: ApiResult<List<T>>, crossinline id: (T) -> String): ApiResult<List<T>> {
        val user = userId() ?: return result
        return when (result) {
            is ApiResult.Success -> { local?.replace(user, kind, result.data, id); result }
            is ApiResult.Error -> if (isOffline(result)) local?.readAll<T>(user, kind)?.let { ApiResult.Success(it) } ?: result else result
        }
    }

    private suspend fun <T> write(kind: String, id: String, request: Any, result: ApiResult<T>, responseId: (T) -> String): ApiResult<T> {
        val user = userId() ?: return result
        return when (result) {
            is ApiResult.Success -> { local?.save(user, kind, responseId(result.data), result.data); result }
            is ApiResult.Error -> if (isOffline(result)) {
                val value: Any = when (request) {
                    is ClientRequest -> ClientResponse(id, request.name, request.email, request.phoneNumber, request.address, request.notes)
                    is JobRequest -> JobResponse(id, request.clientId, local?.read<ClientResponse>(user, "clients", request.clientId)?.name.orEmpty(), request.title, request.description, request.status, request.startDate, request.dueDate, request.completedDate, request.agreedAmount, "ZAR")
                    is CreateExpenseRequest -> ExpenseResponse(id, request.amount, "ZAR", request.expenseDate, request.category, request.description)
                    is CreateIncomeRequest -> IncomeResponse(id, request.amount, "ZAR", request.dateReceived, request.paymentMethod, request.description)
                    else -> return result
                }
                local?.save(user, kind, id, value)
                local?.enqueue(user, kind, "upsert", id, request)
                @Suppress("UNCHECKED_CAST")
                ApiResult.Success(value as T)
            } else result
        }
    }
}
