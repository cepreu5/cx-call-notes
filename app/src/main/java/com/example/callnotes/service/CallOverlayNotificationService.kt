package com.example.callnotes.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.app.Notification
import android.graphics.Color
import android.media.RingtoneManager
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.callnotes.R
import com.example.callnotes.ui.PostCallNoteActivity
import android.util.Log

class CallOverlayNotificationService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val name = intent?.getStringExtra(EXTRA_NAME) ?: return START_NOT_STICKY
        val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        val note = intent.getStringExtra(EXTRA_NOTE) ?: ""
        val tags = intent.getStringExtra(EXTRA_TAGS) ?: ""

        Log.d("CXCalls", "CallOverlayNotificationService: name=$name, phone=$phone")

        acquireWakeLock()
        showNotification(name, phone, note, tags)

        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "cxcalls:call_overlay_lock"
        ).apply {
            acquire(60 * 1000L)
        }
        Log.d("CXCalls", "WakeLock acquired")
    }

    private fun showNotification(name: String, phone: String, note: String, tags: String) {
        val contentText = buildString {
            append(name)
            if (!note.isBlank()) append("\n$note")
            if (!tags.isBlank()) append("\n$tags")
        }

        val contentIntent = Intent(this, PostCallNoteActivity::class.java).apply {
            putExtra(PostCallNoteActivity.EXTRA_PHONE, phone)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val fullScreenIntent = PendingIntent.getActivity(
            this, NOTIFICATION_ID, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, CallOverlayNotificationService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this, NOTIFICATION_ID + 1, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Входящо обаждане")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(false)
            .setOngoing(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setContentIntent(fullScreenIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_launcher_foreground, "Отвори", fullScreenIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Затвори", dismissPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d("CXCalls", "Foreground notification started")
    }

    override fun onDestroy() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
        Log.d("CXCalls", "CallOverlayNotificationService destroyed, WakeLock released")
        super.onDestroy()
    }

    companion object {
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_TAGS = "extra_tags"
        const val ACTION_DISMISS = "action_dismiss"
        const val CHANNEL_ID = "incoming_call_channel"
        const val NOTIFICATION_ID = 1002

        fun start(context: Context, name: String, phone: String, note: String, tags: String) {
            val intent = Intent(context, CallOverlayNotificationService::class.java).apply {
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_PHONE, phone)
                putExtra(EXTRA_NOTE, note)
                putExtra(EXTRA_TAGS, tags)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CallOverlayNotificationService::class.java))
        }
    }
}
