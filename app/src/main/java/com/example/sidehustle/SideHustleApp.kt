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

    val repository: SideHustleRepository by lazy { SideHustleRepositoryImpl() }
    val pushRegistration by lazy { com.example.sidehustle.notifications.PushRegistration(this) }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        pushRegistration.createChannel()
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
            pushRegistration.onSessionChanged(auth.currentUser?.uid)
        }
        Log.d(TAG, "Firebase initialised")
    }

    companion object {
        private const val TAG = "SideHustle"
    }
}
