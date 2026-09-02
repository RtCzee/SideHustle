package com.example.sidehustle.data.repository

import com.example.sidehustle.data.model.HealthResponse
import com.example.sidehustle.data.model.MeResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.data.remote.RemoteDataSource

interface SideHustleRepository {
    suspend fun fetchHealth(): ApiResult<HealthResponse>
    suspend fun fetchMe(): ApiResult<MeResponse>
}

class SideHustleRepositoryImpl(
    private val remote: RemoteDataSource = RemoteDataSource(),
) : SideHustleRepository {

    override suspend fun fetchHealth(): ApiResult<HealthResponse> = remote.fetchHealth()

    override suspend fun fetchMe(): ApiResult<MeResponse> = remote.fetchMe()
}
