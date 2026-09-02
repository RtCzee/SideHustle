package com.example.sidehustle.network

import com.example.sidehustle.data.model.HealthResponse
import com.example.sidehustle.data.model.MeResponse
import retrofit2.http.GET

interface SideHustleApi {

    @GET("health")
    suspend fun getHealth(): HealthResponse

    @GET("me")
    suspend fun getMe(): MeResponse
}
