package com.example.sidehustle

import android.app.Application
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.sidehustle.data.repository.SideHustleRepository
import com.example.sidehustle.data.repository.SideHustleRepositoryImpl
import com.example.sidehustle.data.local.LocalDataSource
import com.example.sidehustle.data.local.SideHustleDatabase
import com.example.sidehustle.data.remote.RemoteDataSource
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.data.model.SyncOperation
import com.example.sidehustle.data.model.SyncRequest
import com.example.sidehustle.data.model.ClientResponse
import com.example.sidehustle.data.model.JobResponse
import com.example.sidehustle.data.model.ExpenseResponse
import com.example.sidehustle.data.model.IncomeResponse
import android.net.ConnectivityManager
import android.net.Network
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val local by lazy { LocalDataSource(SideHustleDatabase.get(this).localDao()) }
    private val remote by lazy { RemoteDataSource() }
    private val defaultRepository: SideHustleRepository by lazy { SideHustleRepositoryImpl(remote, local) }
    private val _syncStatus = MutableStateFlow(SyncStatus.ONLINE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /** Device form checks replace transport so they cannot write to the user's real account. */
    @androidx.annotation.VisibleForTesting
    internal var repositoryOverride: SideHustleRepository? = null
    val repository: SideHustleRepository get() = repositoryOverride ?: defaultRepository

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        val connectivity = getSystemService(ConnectivityManager::class.java)
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _syncStatus.value = SyncStatus.SYNCING
                sessionScope.launch { syncPending() }
            }
            override fun onLost(network: Network) { _syncStatus.value = SyncStatus.OFFLINE }
        })
        if (connectivity.activeNetwork == null) _syncStatus.value = SyncStatus.OFFLINE
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
            if (auth.currentUser != null) sessionScope.launch { syncPending() }
        }
        Log.d(TAG, "Firebase initialised")
    }

    private suspend fun syncPending() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: run { _syncStatus.value = SyncStatus.ONLINE; return }
        _syncStatus.value = SyncStatus.SYNCING
        while (true) {
            val writes = local.pending(user).take(500)
            if (writes.isEmpty()) break
            val request = SyncRequest(writes.map { item ->
                @Suppress("UNCHECKED_CAST")
                val payload = Gson().fromJson(item.payload, Map::class.java) as Map<String, Any?>
                SyncOperation(item.operationId, item.kind, item.action, item.recordId, payload)
            })
            when (val result = remote.sync(request)) {
                is ApiResult.Success -> {
                    if (result.data.acceptedOperationIds.isEmpty()) { _syncStatus.value = SyncStatus.OFFLINE; return }
                    result.data.acceptedOperationIds.forEach { local.remove(it) }
                }
                is ApiResult.Error -> { _syncStatus.value = SyncStatus.OFFLINE; return }
            }
        }
        refreshCoreData(user)
        _syncStatus.value = if (local.pending(user).isEmpty()) SyncStatus.ONLINE else SyncStatus.SYNCING
    }

    private suspend fun refreshCoreData(user: String) {
        val clients = remote.fetchClients(); if (clients is ApiResult.Success) local.replace(user, "clients", clients.data) { it.clientId }
        val jobs = remote.fetchJobs(); if (jobs is ApiResult.Success) local.replace(user, "jobs", jobs.data) { it.jobId }
        val expenses = remote.fetchExpenses(); if (expenses is ApiResult.Success) local.replace(user, "expenses", expenses.data) { it.expenseId }
        val income = remote.fetchIncome(); if (income is ApiResult.Success) local.replace(user, "income", income.data) { it.id }
    }

    companion object {
        private const val TAG = "SideHustle"
    }
}

enum class SyncStatus { ONLINE, OFFLINE, SYNCING }
