package com.example.sidehustle.network

import com.example.sidehustle.data.model.CreateProfileRequest
import com.example.sidehustle.data.model.DashboardResponse
import com.example.sidehustle.data.model.HealthResponse
import com.example.sidehustle.data.model.UserProfileResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SideHustleApi {

    @GET("health")
    suspend fun getHealth(): HealthResponse

    @GET("me")
    suspend fun getProfile(): UserProfileResponse

    @POST("me")
    suspend fun createProfile(@Body body: CreateProfileRequest): UserProfileResponse

    @GET("dashboard")
    suspend fun getDashboard(): DashboardResponse
}
