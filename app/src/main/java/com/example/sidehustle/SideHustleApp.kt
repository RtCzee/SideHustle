package com.example.sidehustle

import android.app.Application
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.sidehustle.data.repository.SideHustleRepository
import com.example.sidehustle.data.repository.SideHustleRepositoryImpl
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Starts Firebase when the process launches so Auth is ready before login/register.
 */
class SideHustleApp : Application() {

    val pushRegistration by lazy { com.example.sidehustle.notifications.PushRegistration(this) }
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val defaultRepository: SideHustleRepository by lazy { SideHustleRepositoryImpl() }

    /** Device form checks replace transport so they cannot write to the user's real account. */
    @androidx.annotation.VisibleForTesting
    internal var repositoryOverride: SideHustleRepository? = null
    val repository: SideHustleRepository get() = repositoryOverride ?: defaultRepository

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        pushRegistration.createChannel()
        var previousUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (previousUserId != null && auth.currentUser == null) {
                sessionScope.launch {
                    try { CredentialManager.create(this@SideHustleApp).clearCredentialState(ClearCredentialStateRequest()) }
                    catch (_: Exception) { Log.w(TAG, "Could not clear credential provider session") }
                }
            }
            previousUserId = auth.currentUser?.uid
            pushRegistration.onSessionChanged(auth.currentUser?.uid)
        }
        Log.d(TAG, "Firebase initialised")
    }

    companion object {
        private const val TAG = "SideHustle"
    }
}
