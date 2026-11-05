package com.example.sidehustle.ui.auth

import android.app.Activity
import androidx.annotation.StringRes
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.sidehustle.R
import com.example.sidehustle.data.model.UserProfileResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.data.repository.SideHustleRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

sealed class GoogleSignInResult {
    data class Success(val profile: UserProfileResponse) : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
    data class Failure(@param:StringRes @get:StringRes val message: Int, val apiError: ApiResult.Error? = null) : GoogleSignInResult()
}

/** Explicit button flow supports both returning Google users and first-time registration. */
class GoogleSignIn(
    private val activity: Activity,
    private val repository: SideHustleRepository,
) {
    suspend fun signIn(): GoogleSignInResult {
        val clientId = activity.getString(R.string.google_web_client_id).trim()
        if (clientId.isBlank()) return GoogleSignInResult.Failure(R.string.error_google_not_configured)
        return googleSignInAttempt {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build()).build()
            val response = CredentialManager.create(activity).getCredential(activity, request)
            val credential = response.credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return@googleSignInAttempt GoogleSignInResult.Failure(R.string.error_google_credential)
            }
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            val auth = FirebaseAuth.getInstance()
            val user = auth.signInWithCredential(GoogleAuthProvider.getCredential(google.idToken, null)).await().user
                ?: return@googleSignInAttempt GoogleSignInResult.Failure(R.string.error_google_sign_in)
            when (val profile = AuthProfileLoader(repository, activity.getString(R.string.error_profile_name_required)).load(user.displayName, user.email)) {
                is ApiResult.Success -> GoogleSignInResult.Success(profile.data)
                is ApiResult.Error -> GoogleSignInResult.Failure(R.string.error_google_profile, profile)
            }
        }
    }
}

/** System-sheet cancellation is a normal outcome; coroutine cancellation belongs to the caller. */
internal suspend fun googleSignInAttempt(action: suspend () -> GoogleSignInResult): GoogleSignInResult =
    try {
        action()
    } catch (_: GetCredentialCancellationException) {
        GoogleSignInResult.Cancelled
    } catch (cancelled: CancellationException) {
        // Fragment teardown is not a visible sign-in failure.
        throw cancelled
    } catch (_: NoCredentialException) {
        GoogleSignInResult.Failure(R.string.error_google_no_account)
    } catch (_: GetCredentialProviderConfigurationException) {
        GoogleSignInResult.Failure(R.string.error_google_unavailable)
    } catch (_: GoogleIdTokenParsingException) {
        GoogleSignInResult.Failure(R.string.error_google_credential)
    } catch (_: FirebaseNetworkException) {
        GoogleSignInResult.Failure(R.string.error_network)
    } catch (error: FirebaseAuthException) {
        GoogleSignInResult.Failure(googleAuthErrorMessage(error.errorCode))
    } catch (_: GetCredentialException) {
        GoogleSignInResult.Failure(R.string.error_google_sign_in)
    } catch (_: Exception) {
        GoogleSignInResult.Failure(R.string.error_google_sign_in)
    }

@StringRes
internal fun googleAuthErrorMessage(code: String?): Int = when (code) {
    "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "ERROR_EMAIL_ALREADY_IN_USE",
    "ERROR_CREDENTIAL_ALREADY_IN_USE" -> R.string.error_google_existing_account
    "ERROR_OPERATION_NOT_ALLOWED", "ERROR_INVALID_OAUTH_CLIENT_ID" -> R.string.error_google_not_configured
    "ERROR_NETWORK_REQUEST_FAILED" -> R.string.error_network
    "ERROR_USER_DISABLED" -> R.string.error_google_disabled
    else -> R.string.error_google_sign_in
}
