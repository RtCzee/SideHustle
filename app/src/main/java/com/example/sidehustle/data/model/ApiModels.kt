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

/**
 * Body for PUT /me — settings screen save (issue: Build the Settings Screen).
 * phoneNumber is a plain (possibly empty) String, not String? — Retrofit's default Gson
 * setup omits null fields from the JSON body entirely, so a null here would silently fail
 * to clear the phone number server-side. Sending "" clears it instead.
 */
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

data class ClientResponse(
    @SerializedName("client_id")
    val clientId: String,
    val name: String,
    val email: String? = null,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
)

/**
 * Body for POST /clients and PUT /clients/{id} (issue: Build Client Management).
 * email/phoneNumber/address/notes are plain Strings (possibly empty), not String? — same
 * reasoning as UpdateProfileRequest: default Gson omits null fields, so null here would
 * silently fail to clear a field on an edit.
 */
data class ClientRequest(
    val name: String,
    val email: String,
    @SerializedName("phone_number")
    val phoneNumber: String,
    val address: String,
    val notes: String,
)

data class DeleteResult(
    val deleted: Boolean = false,
)

data class JobResponse(
    @SerializedName("job_id")
    val jobId: String,
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("client_name")
    val clientName: String,
    val title: String,
    val description: String? = null,
    val status: String,
    @SerializedName("start_date")
    val startDate: String? = null,
    @SerializedName("due_date")
    val dueDate: String? = null,
    @SerializedName("completed_date")
    val completedDate: String? = null,
    @SerializedName("agreed_amount")
    val agreedAmount: Double,
    val currency: String,
)

/**
 * Body for POST /jobs and PUT /jobs/{id} (issue: Build Job/Project Management).
 * Date fields are plain (possibly empty) Strings, not String? — same reasoning as
 * ClientRequest/UpdateProfileRequest: default Gson omits null fields, so null here
 * would silently fail to clear a date on an edit.
 */
data class JobRequest(
    @SerializedName("client_id")
    val clientId: String,
    val title: String,
    val description: String,
    val status: String,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("due_date")
    val dueDate: String,
    @SerializedName("completed_date")
    val completedDate: String,
    @SerializedName("agreed_amount")
    val agreedAmount: Double,
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
