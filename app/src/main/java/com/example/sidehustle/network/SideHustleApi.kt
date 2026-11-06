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
import com.example.sidehustle.data.model.CreateExpenseRequest
import com.example.sidehustle.data.model.ExpenseResponse
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
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.sidehustle.data.model.SyncRequest
import com.example.sidehustle.data.model.SyncResponse

interface SideHustleApi {

    @POST("sync")
    suspend fun sync(@Body body: SyncRequest): SyncResponse

    @PUT("me/push-device")
    suspend fun registerPushDevice(@Body body: com.example.sidehustle.data.model.PushDeviceRequest): com.example.sidehustle.data.model.PushDeviceResponse

    @GET("attachments/{type}/{id}")
    suspend fun getAttachments(@Path("type") type: String, @Path("id") id: String): List<com.example.sidehustle.data.model.Attachment>

    @POST("attachments/{type}/{id}")
    suspend fun addAttachment(@Path("type") type: String, @Path("id") id: String,
        @Body body: com.example.sidehustle.data.model.AttachmentRequest): com.example.sidehustle.data.model.Attachment

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

    // Expenses
    @GET("expenses")
    suspend fun getExpenses(): List<ExpenseResponse>

    @POST("expenses")
    suspend fun createExpense(@Body body: CreateExpenseRequest): ExpenseResponse

    // Income
    @GET("income/options")
    suspend fun getIncomeOptions(): IncomeOptionsResponse

    @GET("income")
    suspend fun getIncome(): List<IncomeResponse>

    @POST("income")
    suspend fun createIncome(@Body body: CreateIncomeRequest): IncomeResponse

    // Invoices
    @GET("invoices/clients")
    suspend fun getInvoiceClients(): List<InvoiceClient>

    @GET("invoices/jobs")
    suspend fun getInvoiceJobs(@Query("client_id") clientId: String): List<InvoiceJob>

    @GET("invoices")
    suspend fun getInvoices(): List<InvoiceResponse>

    @POST("invoices")
    suspend fun createInvoice(@Body body: CreateInvoiceRequest): InvoiceResponse

    @PATCH("invoices/{id}/status")
    suspend fun updateInvoiceStatus(@Path("id") invoiceId: String, @Body body: UpdateInvoiceStatusRequest): InvoiceResponse
}
