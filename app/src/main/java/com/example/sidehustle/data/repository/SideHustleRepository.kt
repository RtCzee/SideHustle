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
) : SideHustleRepository {

    override suspend fun fetchHealth(): ApiResult<HealthResponse> = remote.fetchHealth()

    override suspend fun fetchProfile(): ApiResult<UserProfileResponse> = remote.fetchProfile()

    override suspend fun createProfile(request: CreateProfileRequest): ApiResult<UserProfileResponse> =
        remote.createProfile(request)

    override suspend fun updateProfile(request: UpdateProfileRequest): ApiResult<UserProfileResponse> =
        remote.updateProfile(request)

    override suspend fun fetchDashboard(): ApiResult<DashboardResponse> = remote.fetchDashboard()

    override suspend fun fetchClients(): ApiResult<List<ClientResponse>> = remote.fetchClients()

    override suspend fun fetchClient(clientId: String): ApiResult<ClientResponse> =
        remote.fetchClient(clientId)

    override suspend fun createClient(request: ClientRequest): ApiResult<ClientResponse> =
        remote.createClient(request)

    override suspend fun updateClient(clientId: String, request: ClientRequest): ApiResult<ClientResponse> =
        remote.updateClient(clientId, request)

    override suspend fun deleteClient(clientId: String): ApiResult<DeleteResult> =
        remote.deleteClient(clientId)

    override suspend fun fetchJobs(): ApiResult<List<JobResponse>> = remote.fetchJobs()

    override suspend fun fetchJob(jobId: String): ApiResult<JobResponse> = remote.fetchJob(jobId)

    override suspend fun createJob(request: JobRequest): ApiResult<JobResponse> =
        remote.createJob(request)

    override suspend fun updateJob(jobId: String, request: JobRequest): ApiResult<JobResponse> =
        remote.updateJob(jobId, request)

    override suspend fun fetchExpenses(): ApiResult<List<ExpenseResponse>> = remote.fetchExpenses()

    override suspend fun createExpense(request: CreateExpenseRequest): ApiResult<ExpenseResponse> =
        remote.createExpense(request)

    override suspend fun fetchIncomeOptions(): ApiResult<IncomeOptionsResponse> =
        remote.fetchIncomeOptions()

    override suspend fun fetchIncome(): ApiResult<List<IncomeResponse>> = remote.fetchIncome()

    override suspend fun createIncome(request: CreateIncomeRequest): ApiResult<IncomeResponse> =
        remote.createIncome(request)

    override suspend fun fetchInvoiceClients(): ApiResult<List<InvoiceClient>> =
        remote.fetchInvoiceClients()

    override suspend fun fetchInvoiceJobs(clientId: String): ApiResult<List<InvoiceJob>> =
        remote.fetchInvoiceJobs(clientId)

    override suspend fun fetchInvoices(): ApiResult<List<InvoiceResponse>> = remote.fetchInvoices()

    override suspend fun createInvoice(request: CreateInvoiceRequest): ApiResult<InvoiceResponse> =
        remote.createInvoice(request)

    override suspend fun updateInvoiceStatus(invoiceId: String, status: String): ApiResult<InvoiceResponse> =
        remote.updateInvoiceStatus(invoiceId, status)
}
