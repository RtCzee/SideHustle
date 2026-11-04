package com.example.sidehustle.ui.auth

import com.example.sidehustle.data.model.CreateProfileRequest
import com.example.sidehustle.data.model.UserProfileResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.data.repository.SideHustleRepository

/** Google and email sessions resolve the same API resource, keyed by the Firebase UID. */
class AuthProfileLoader(
    private val fetch: suspend () -> ApiResult<UserProfileResponse>,
    private val create: suspend (CreateProfileRequest) -> ApiResult<UserProfileResponse>,
) {
    constructor(repository: SideHustleRepository) : this(repository::fetchProfile, repository::createProfile)

    suspend fun load(displayName: String?, email: String?): ApiResult<UserProfileResponse> {
        val existing = fetch()
        if (existing !is ApiResult.Error || existing.httpCode != 404) return existing
        val name = displayName?.trim()?.takeIf { it.isNotEmpty() }
            ?: email?.substringBefore('@')?.trim()?.takeIf { it.isNotEmpty() }
            ?: return ApiResult.Error("Add a name to your account before signing in.")
        return when (val created = create(CreateProfileRequest(fullName = name.take(255)))) {
            // Another request/device may finish registration between GET and POST.
            is ApiResult.Error -> if (created.httpCode == 409) fetch() else created
            is ApiResult.Success -> created
        }
    }
}
