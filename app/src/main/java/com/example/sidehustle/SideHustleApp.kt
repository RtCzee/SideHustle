package com.example.sidehustle

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Starts Firebase when the process launches so Auth is ready before login/register.
 */
class SideHustleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Log.d(TAG, "Firebase initialised")
    }

    companion object {
        private const val TAG = "SideHustle"
    }
}
