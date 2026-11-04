package com.example.sidehustle.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.sidehustle.MainActivity
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ReminderMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        if (token.isNotBlank()) (application as SideHustleApp).pushRegistration.syncAsync()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val target = ReminderTarget.from(message.data["type"], message.data["user_id"], message.data["job_id"]) ?: return
        val registration = (application as SideHustleApp).pushRegistration
        if (!target.canShow(FirebaseAuth.getInstance().currentUser?.uid, registration.enabled(target.userId))) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        registration.createChannel()
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "com.example.sidehustle.OPEN_JOB.${target.jobId}"
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_USER_ID, target.userId)
            putExtra(EXTRA_JOB_ID, target.jobId)
        }
        val contentIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val body = message.data["body"]?.take(1000) ?: getString(R.string.reminder_body_fallback)
        val notification = NotificationCompat.Builder(this, PushRegistration.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.reminder_job_due_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
        try { manager.notify(target.jobId, 0, notification) }
        catch (_: SecurityException) { /* Permission may be revoked between the check and notify. */ }
    }

    companion object {
        const val EXTRA_USER_ID = "push_user_id"
        const val EXTRA_JOB_ID = "push_job_id"
    }
}
