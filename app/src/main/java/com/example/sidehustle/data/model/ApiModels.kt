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
