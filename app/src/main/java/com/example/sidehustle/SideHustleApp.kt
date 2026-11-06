package com.example.sidehustle

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.sidehustle.data.local.LocalDataSource
import com.example.sidehustle.data.local.SideHustleDatabase
import com.example.sidehustle.data.remote.RemoteDataSource
import com.example.sidehustle.data.repository.SideHustleRepository
import com.example.sidehustle.data.repository.SideHustleRepositoryImpl
import com.example.sidehustle.notifications.PushRegistration
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SideHustleApp : Application() {
    val pushRegistration by lazy { PushRegistration(this) }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
    private val _syncStatus = MutableStateFlow(SyncStatus.ONLINE)
    val syncStatus = _syncStatus.asStateFlow()

    private val defaultRepository by lazy {
        SideHustleRepositoryImpl(
            RemoteDataSource(), LocalDataSource(SideHustleDatabase.get(this)),
            currentUserId = { auth.currentUser?.uid },
            currentEmail = { auth.currentUser?.email },
            setStatus = { _syncStatus.value = it },
            currentDisplayName = { auth.currentUser?.displayName },
        )
    }

    /** Device form checks replace transport so they cannot write to the user's real account. */
    @androidx.annotation.VisibleForTesting
    internal var repositoryOverride: SideHustleRepository? = null
    val repository: SideHustleRepository get() = repositoryOverride ?: defaultRepository

    internal val hasUiSession: Boolean
        get() = auth.currentUser != null || (BuildConfig.DEBUG && repositoryOverride != null)

    internal suspend fun fetchAttachments(type: String, recordId: String) =
        defaultRepository.fetchAttachments(type, recordId)

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        val connectivity = getSystemService(ConnectivityManager::class.java)
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { requestRefresh() }
            override fun onLost(network: Network) {
                if (auth.currentUser != null && connectivity.activeNetwork == null) {
                    _syncStatus.value = SyncStatus.OFFLINE
                }
            }
        })

        sessionScope.launch {
            while (true) {
                val requested = withTimeoutOrNull(30_000) { refreshRequests.receive(); true } == true
                if (repositoryOverride != null || auth.currentUser == null || connectivity.activeNetwork == null) continue
                if (!requested && _syncStatus.value == SyncStatus.ONLINE) continue
                try {
                    defaultRepository.refresh()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    _syncStatus.value = SyncStatus.BLOCKED
                    Log.w(TAG, "Could not refresh local data", error)
                }
            }
        }

        pushRegistration.createChannel()
        var previousUserId = auth.currentUser?.uid
        auth.addAuthStateListener { session ->
            val user = session.currentUser?.uid
            if (previousUserId != null && user == null) {
                sessionScope.launch {
                    try {
                        CredentialManager.create(this@SideHustleApp).clearCredentialState(ClearCredentialStateRequest())
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        Log.w(TAG, "Could not clear credential provider session")
                    }
                }
            }
            previousUserId = user
            pushRegistration.onSessionChanged(user)
            if (user == null) _syncStatus.value = SyncStatus.ONLINE
            else if (connectivity.activeNetwork == null) _syncStatus.value = SyncStatus.OFFLINE
            else requestRefresh()
        }
    }

    private fun requestRefresh() { refreshRequests.trySend(Unit) }

    private companion object {
        const val TAG = "SideHustle"
    }
}

enum class SyncStatus { ONLINE, OFFLINE, SYNCING, BLOCKED }
