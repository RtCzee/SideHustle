package com.example.sidehustle.network

import com.example.sidehustle.data.model.ClientRequest
import com.example.sidehustle.data.model.ClientResponse
import com.example.sidehustle.data.model.CreateProfileRequest
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
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface SideHustleApi {

    @GET("health")
    suspend fun getHealth(): HealthResponse

    @GET("me")
    suspend fun getProfile(): UserProfileResponse

    @POST("me")
    suspend fun createProfile(@Body body: CreateProfileRequest): UserProfileResponse

    @PUT("me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): UserProfileResponse

    @GET("dashboard")
    suspend fun getDashboard(): DashboardResponse

    @GET("clients")
    suspend fun getClients(): List<ClientResponse>

    @GET("clients/{id}")
    suspend fun getClient(@Path("id") clientId: String): ClientResponse

    @POST("clients")
    suspend fun createClient(@Body body: ClientRequest): ClientResponse

    @PUT("clients/{id}")
    suspend fun updateClient(@Path("id") clientId: String, @Body body: ClientRequest): ClientResponse

    @DELETE("clients/{id}")
    suspend fun deleteClient(@Path("id") clientId: String): DeleteResult

    @GET("jobs")
    suspend fun getJobs(): List<JobResponse>

    @GET("jobs/{id}")
    suspend fun getJob(@Path("id") jobId: String): JobResponse

    @POST("jobs")
    suspend fun createJob(@Body body: JobRequest): JobResponse

    @PUT("jobs/{id}")
    suspend fun updateJob(@Path("id") jobId: String, @Body body: JobRequest): JobResponse
}
