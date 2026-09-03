package com.example.sidehustle.data.repository

import com.example.sidehustle.data.model.CreateProfileRequest
import com.example.sidehustle.data.model.HealthResponse
import com.example.sidehustle.data.model.UserProfileResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.data.remote.RemoteDataSource

interface SideHustleRepository {
    suspend fun fetchHealth(): ApiResult<HealthResponse>
    suspend fun fetchProfile(): ApiResult<UserProfileResponse>
    suspend fun createProfile(request: CreateProfileRequest): ApiResult<UserProfileResponse>
}

class SideHustleRepositoryImpl(
    private val remote: RemoteDataSource = RemoteDataSource(),
) : SideHustleRepository {

    override suspend fun fetchHealth(): ApiResult<HealthResponse> = remote.fetchHealth()

    override suspend fun fetchProfile(): ApiResult<UserProfileResponse> = remote.fetchProfile()

    override suspend fun createProfile(request: CreateProfileRequest): ApiResult<UserProfileResponse> =
        remote.createProfile(request)
}
