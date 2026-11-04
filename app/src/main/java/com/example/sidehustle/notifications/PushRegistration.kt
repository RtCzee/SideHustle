package com.example.sidehustle.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.sidehustle.R
import com.example.sidehustle.data.model.PushDeviceRequest
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.data.remote.RemoteDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PushRegistration(private val context: Context) {
    private val prefs = context.getSharedPreferences("push", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val remote by lazy { RemoteDataSource() }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    fun enabled(userId: String): Boolean = prefs.getBoolean("enabled_$userId", false)

    fun setEnabled(userId: String, enabled: Boolean) {
        prefs.edit().putBoolean("enabled_$userId", enabled).apply()
        if (!enabled) clearNotifications()
    }

    fun clearNotifications() { NotificationManagerCompat.from(context).cancelAll() }

    fun onSessionChanged(userId: String?) {
        if (prefs.getString("active_uid", null) != userId) clearNotifications()
        prefs.edit().putString("active_uid", userId).apply()
        if (userId != null) syncAsync()
    }

    fun permissionWasRequested() = prefs.getBoolean("permission_requested", false)
    fun markPermissionRequested() { prefs.edit().putBoolean("permission_requested", true).apply() }

    fun syncAsync() { scope.launch { sync() } }

    /** Best effort; retry on token refresh, login, resume, and settings changes. Never blocks login. */
    suspend fun sync(): Boolean = mutex.withLock {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withLock false
        try {
            val profile = remote.fetchProfile()
            if (profile !is ApiResult.Success || FirebaseAuth.getInstance().currentUser?.uid != uid) return@withLock false
            setEnabled(uid, profile.data.notificationsEnabled)
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNullOrBlank() || FirebaseAuth.getInstance().currentUser?.uid != uid) return@withLock enabled(uid)
            val deviceId = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
                prefs.edit().putString("device_id", it).apply()
            }
            val result = remote.registerPushDevice(PushDeviceRequest(deviceId, uid, token,
                NotificationManagerCompat.from(context).areNotificationsEnabled()))
            if (result is ApiResult.Error) Log.w("SideHustle", "Push registration will retry on next sync")
            enabled(uid)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            // Play Services/network/token may be unavailable. Other app features remain usable.
            Log.w("SideHustle", "FCM token unavailable; will retry on next sync")
            enabled(uid)
        }
    }

    companion object { const val CHANNEL_ID = "job_reminders" }
}
