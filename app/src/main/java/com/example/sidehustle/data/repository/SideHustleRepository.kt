package com.example.sidehustle.data.repository

import com.example.sidehustle.data.model.CreateProfileRequest
import com.example.sidehustle.data.model.DashboardResponse
import com.example.sidehustle.data.model.HealthResponse
import com.example.sidehustle.data.model.UserProfileResponse
import com.example.sidehustle.data.model.InvoiceClient
import com.example.sidehustle.data.model.InvoiceJob
import com.example.sidehustle.data.model.InvoiceResponse
import com.example.sidehustle.data.model.CreateInvoiceRequest
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.data.remote.RemoteDataSource

interface SideHustleRepository {
    suspend fun fetchHealth(): ApiResult<HealthResponse>
    suspend fun fetchProfile(): ApiResult<UserProfileResponse>
    suspend fun createProfile(request: CreateProfileRequest): ApiResult<UserProfileResponse>
    suspend fun fetchDashboard(): ApiResult<DashboardResponse>
    suspend fun fetchInvoiceClients(): ApiResult<List<InvoiceClient>>
    suspend fun fetchInvoiceJobs(clientId: String): ApiResult<List<InvoiceJob>>
    suspend fun fetchInvoices(): ApiResult<List<InvoiceResponse>>
    suspend fun createInvoice(request: CreateInvoiceRequest): ApiResult<InvoiceResponse>
    suspend fun updateInvoiceStatus(id: String, status: String): ApiResult<InvoiceResponse>
}

class SideHustleRepositoryImpl(
    private val remote: RemoteDataSource = RemoteDataSource(),
) : SideHustleRepository {

    override suspend fun fetchHealth(): ApiResult<HealthResponse> = remote.fetchHealth()

    override suspend fun fetchProfile(): ApiResult<UserProfileResponse> = remote.fetchProfile()

    override suspend fun createProfile(request: CreateProfileRequest): ApiResult<UserProfileResponse> =
        remote.createProfile(request)

    override suspend fun fetchDashboard(): ApiResult<DashboardResponse> = remote.fetchDashboard()
    override suspend fun fetchInvoiceClients() = remote.fetchInvoiceClients()
    override suspend fun fetchInvoiceJobs(clientId: String) = remote.fetchInvoiceJobs(clientId)
    override suspend fun fetchInvoices() = remote.fetchInvoices()
    override suspend fun createInvoice(request: CreateInvoiceRequest) = remote.createInvoice(request)
    override suspend fun updateInvoiceStatus(id: String, status: String) = remote.updateInvoiceStatus(id, status)
}
