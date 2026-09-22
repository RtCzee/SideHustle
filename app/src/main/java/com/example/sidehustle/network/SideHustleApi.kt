package com.example.sidehustle.network

import com.example.sidehustle.data.model.CreateProfileRequest
import com.example.sidehustle.data.model.DashboardResponse
import com.example.sidehustle.data.model.HealthResponse
import com.example.sidehustle.data.model.UpdateProfileRequest
import com.example.sidehustle.data.model.CreateExpenseRequest
import com.example.sidehustle.data.model.ExpenseResponse
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
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

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

    @GET("expenses")
    suspend fun getExpenses(): List<ExpenseResponse>

    @POST("expenses")
    suspend fun createExpense(@Body body: CreateExpenseRequest): ExpenseResponse

    @GET("invoices/clients") suspend fun getInvoiceClients(): List<InvoiceClient>
    @GET("invoices/jobs") suspend fun getInvoiceJobs(@Query("client_id") clientId: String): List<InvoiceJob>
    @GET("invoices") suspend fun getInvoices(): List<InvoiceResponse>
    @POST("invoices") suspend fun createInvoice(@Body body: CreateInvoiceRequest): InvoiceResponse
    @PATCH("invoices/{id}/status") suspend fun updateInvoiceStatus(@Path("id") id: String, @Body body: UpdateInvoiceStatusRequest): InvoiceResponse

    @GET("income/options") suspend fun getIncomeOptions(): IncomeOptionsResponse
    @GET("income") suspend fun getIncome(): List<IncomeResponse>
    @POST("income") suspend fun createIncome(@Body body: CreateIncomeRequest): IncomeResponse
}
