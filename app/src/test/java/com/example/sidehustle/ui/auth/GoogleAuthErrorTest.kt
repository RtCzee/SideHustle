package com.example.sidehustle.ui.auth

import com.example.sidehustle.R
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GoogleAuthErrorTest {
    @Test fun `cancelled Google account sheet returns normally so controls can be restored`() = runBlocking {
        val result = googleSignInAttempt { throw GetCredentialCancellationException() }
        assertEquals(GoogleSignInResult.Cancelled, result)
    }

    @Test fun `missing Google account and unexpected provider failure return safe failures`() = runBlocking {
        val missing = googleSignInAttempt { throw NoCredentialException() }
        assertEquals(GoogleSignInResult.Failure(R.string.error_google_no_account), missing)
        val unknown = googleSignInAttempt { error("Private provider diagnostic") }
        assertEquals(GoogleSignInResult.Failure(R.string.error_google_sign_in), unknown)
    }

    @Test fun `destroying auth screen propagates coroutine cancellation`() = runBlocking {
        val cancellation = CancellationException("View destroyed")
        try { googleSignInAttempt { throw cancellation }; fail("Cancellation swallowed") }
        catch (actual: CancellationException) { assertSame(cancellation, actual) }
    }

    @Test fun `account collisions ask for the original sign in method`() {
        for (code in listOf("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_CREDENTIAL_ALREADY_IN_USE")) {
            assertEquals(R.string.error_google_existing_account, googleAuthErrorMessage(code))
        }
    }

    @Test fun `configuration network disabled and unexpected failures have safe messages`() {
        assertEquals(R.string.error_google_not_configured, googleAuthErrorMessage("ERROR_OPERATION_NOT_ALLOWED"))
        assertEquals(R.string.error_network, googleAuthErrorMessage("ERROR_NETWORK_REQUEST_FAILED"))
        assertEquals(R.string.error_google_disabled, googleAuthErrorMessage("ERROR_USER_DISABLED"))
        assertEquals(R.string.error_google_sign_in, googleAuthErrorMessage(null))
    }
}
