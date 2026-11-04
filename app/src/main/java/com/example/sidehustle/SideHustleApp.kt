package com.example.sidehustle

import android.app.Application
import android.util.Log
import com.example.sidehustle.data.repository.SideHustleRepository
import com.example.sidehustle.data.repository.SideHustleRepositoryImpl
import com.google.firebase.FirebaseApp

/**
 * Starts Firebase when the process launches so Auth is ready before login/register.
 */
class SideHustleApp : Application() {

    private val defaultRepository: SideHustleRepository by lazy { SideHustleRepositoryImpl() }

    /** Device form checks replace transport so they cannot write to the user's real account. */
    @androidx.annotation.VisibleForTesting
    internal var repositoryOverride: SideHustleRepository? = null
    val repository: SideHustleRepository get() = repositoryOverride ?: defaultRepository

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Log.d(TAG, "Firebase initialised")
    }

    companion object {
        private const val TAG = "SideHustle"
    }
}
