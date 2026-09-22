package com.example.sidehustle.data.model

import com.google.gson.annotations.SerializedName

data class HealthResponse(
    val status: String,
    val service: String,
    val database: String? = null,
    @SerializedName("databaseError")
    val databaseError: String? = null,
)

data class UserProfileResponse(
    @SerializedName("user_id")
    val userId: String,
    val email: String,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    @SerializedName("profile_picture_url")
    val profilePictureUrl: String? = null,
    @SerializedName("preferred_currency")
    val preferredCurrency: String,
    @SerializedName("preferred_language")
    val preferredLanguage: String,
    @SerializedName("notifications_enabled")
    val notificationsEnabled: Boolean = true,
)

data class CreateProfileRequest(
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    @SerializedName("preferred_currency")
    val preferredCurrency: String = "ZAR",
    @SerializedName("preferred_language")
    val preferredLanguage: String = "en",
)

data class UpdateProfileRequest(
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("preferred_currency")
    val preferredCurrency: String,
    @SerializedName("preferred_language")
    val preferredLanguage: String,
    @SerializedName("notifications_enabled")
    val notificationsEnabled: Boolean,
)

data class DashboardResponse(
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("preferred_currency")
    val preferredCurrency: String,
    @SerializedName("total_income")
    val totalIncome: Double,
    @SerializedName("total_expenses")
    val totalExpenses: Double,
    @SerializedName("net_profit")
    val netProfit: Double,
    @SerializedName("outstanding_payments")
    val outstandingPayments: Double,
    @SerializedName("completed_jobs_this_month")
    val completedJobsThisMonth: Int,
    @SerializedName("side_hustle_score")
    val sideHustleScore: Int,
)

data class CreateExpenseRequest(
    val amount: Double,
    @SerializedName("expense_date")
    val expenseDate: String,
    val category: String,
    val description: String? = null,
)

data class ExpenseResponse(
    @SerializedName("expense_id")
    val expenseId: String,
    val amount: Double,
    val currency: String,
    @SerializedName("expense_date")
    val expenseDate: String,
    val category: String,
    val description: String? = null,
)

data class ClientResponse(
    @SerializedName("client_id")
    val clientId: String,
    val name: String,
    val email: String? = null,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
)

data class InvoiceClient(@SerializedName("client_id") val id: String, val name: String)

data class InvoiceJob(
    @SerializedName("job_id") val id: String,
    val title: String,
    @SerializedName("agreed_amount") val agreedAmount: Double,
    val currency: String,
)

data class CreateInvoiceRequest(
    @SerializedName("client_id") val clientId: String,
    @SerializedName("job_id") val jobId: String,
)

data class InvoiceResponse(
    @SerializedName("invoice_id") val id: String,
    @SerializedName("invoice_number") val number: String,
    @SerializedName("total_amount") val total: Double,
    val status: String,
    @SerializedName("client_name") val clientName: String? = null,
)

data class UpdateInvoiceStatusRequest(val status: String)
