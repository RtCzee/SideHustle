package com.example.sidehustle.ui.auth

import com.example.sidehustle.data.model.UserProfileResponse
import com.example.sidehustle.data.remote.ApiResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AuthProfileLoaderTest {
    private val profile = UserProfileResponse(
        userId = "google-firebase-uid", email = "lerato@example.com", fullName = "Lerato Molefe",
        preferredCurrency = "ZAR", preferredLanguage = "en",
    )

    @Test fun `existing Google or email user loads profile without creating or overwriting it`() = runBlocking {
        val loader = AuthProfileLoader(fetch = { ApiResult.Success(profile) }, create = { error("Must not create") })
        assertEquals(ApiResult.Success(profile), loader.load("Other name", profile.email))
    }

    @Test fun `first Google sign in creates profile with trimmed Google display name and default settings`() = runBlocking {
        var creates = 0
        val loader = AuthProfileLoader(fetch = { ApiResult.Error("Not found", 404) }, create = {
            creates++
            assertEquals("Lerato Molefe", it.fullName)
            assertEquals("ZAR", it.preferredCurrency)
            assertEquals("en", it.preferredLanguage)
            ApiResult.Success(profile)
        })
        assertEquals(ApiResult.Success(profile), loader.load("  Lerato Molefe  ", profile.email))
        assertEquals(1, creates)
    }

    @Test fun `missing display name uses email local part`() = runBlocking {
        val loader = AuthProfileLoader(fetch = { ApiResult.Error("Not found", 404) }, create = {
            assertEquals("lerato", it.fullName)
            ApiResult.Success(profile)
        })
        assertTrue(loader.load(" ", profile.email) is ApiResult.Success)
    }

    @Test fun `missing account name and email returns an error without an invalid create`() = runBlocking {
        val loader = AuthProfileLoader(fetch = { ApiResult.Error("Not found", 404) }, create = { error("Must not create") })
        assertTrue(loader.load(null, null) is ApiResult.Error)
    }

    @Test fun `fetch failures other than 404 never create a duplicate profile`() = runBlocking {
        for (code in listOf(null, 401, 403, 500, 503)) {
            val failure = ApiResult.Error("Unavailable", code)
            val loader = AuthProfileLoader(fetch = { failure }, create = { error("Must not create") })
            assertSame(failure, loader.load(profile.fullName, profile.email))
        }
    }

    @Test fun `concurrent profile creation conflict reloads existing profile`() = runBlocking {
        var fetches = 0
        val loader = AuthProfileLoader(fetch = {
            if (++fetches == 1) ApiResult.Error("Not found", 404) else ApiResult.Success(profile)
        }, create = { ApiResult.Error("Already exists", 409) })
        assertEquals(ApiResult.Success(profile), loader.load(profile.fullName, profile.email))
        assertEquals(2, fetches)
    }

    @Test fun `API create failure can be retried without losing the profile name`() = runBlocking {
        var attempts = 0
        val loader = AuthProfileLoader(fetch = { ApiResult.Error("Not found", 404) }, create = {
            if (++attempts == 1) ApiResult.Error("Offline") else ApiResult.Success(profile)
        })
        assertTrue(loader.load(profile.fullName, profile.email) is ApiResult.Error)
        assertEquals(ApiResult.Success(profile), loader.load(profile.fullName, profile.email))
    }

    @Test fun `leaving the auth view propagates cancellation instead of showing an error`() = runBlocking {
        val cancellation = CancellationException("View destroyed")
        val loader = AuthProfileLoader(fetch = { throw cancellation }, create = { error("Must not create") })
        try { loader.load(profile.fullName, profile.email); fail("Cancellation swallowed") }
        catch (actual: CancellationException) { assertSame(cancellation, actual) }
    }
}
