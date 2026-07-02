package com.example.callnotes.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.callnotes.data.DatabaseProvider
import com.example.callnotes.data.PhoneNumberNormalizer
import com.example.callnotes.ui.PostCallNoteActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhoneStateReceiver : BroadcastReceiver() {
    companion object {
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var incomingNumber: String? = null
        private var wasRinging = false
        private const val OUTGOING_CALL_TYPE = 2
        private const val CHANNEL_ID = "cx_call_notes_channel"
        const val NOTIFICATION_ID = 9999
        @Volatile var screenedNumber: String? = null
    }
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        Log.d("CXCalls", "PhoneStateReceiver: state=$stateStr, number=$number")
        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                wasRinging = true
                if (number != null) {
                    incomingNumber = number
                } else if (incomingNumber == null && screenedNumber != null) {
                    incomingNumber = screenedNumber
                    Log.d("CXCalls", "Using screenedNumber fallback: $incomingNumber")
                }
                Log.d("CXCalls", "RINGING: incomingNumber=$incomingNumber")
                if (incomingNumber != null) {
                    val phone = PhoneNumberNormalizer.normalize(incomingNumber!!)
                    Log.d("CXCalls", "Normalized: $phone")
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = DatabaseProvider.get(context.applicationContext)
                            val contact = db.contactDao().findByPhone(phone)
                            Log.d("CXCalls", "PhoneStateReceiver DB lookup: contact=${contact?.displayName ?: "NULL"}")
                            if (contact != null) {
                                if (contact.displayName.startsWith("#")) {
                                    Log.d("CXCalls", "PhoneStateReceiver: Contact excluded (starts with #): ${contact.displayName}")
                                } else {
                                    Log.d("CXCalls", "PhoneStateReceiver: KNOWN contact, starting overlay")
                                    val overlayIntent = Intent(context.applicationContext, OverlayService::class.java).apply {
                                        putExtra(OverlayService.EXTRA_NAME, contact.displayName)
                                        putExtra(OverlayService.EXTRA_NOTE, contact.note ?: "")
                                        putExtra(OverlayService.EXTRA_TAGS, contact.tags ?: "")
                                    }
                                    context.applicationContext.startService(overlayIntent)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("CXCalls", "PhoneStateReceiver error", e)
                        }
                    }
                }
                lastState = TelephonyManager.CALL_STATE_RINGING
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                lastState = TelephonyManager.CALL_STATE_OFFHOOK
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (wasRinging && incomingNumber != null) {
                    val phone = PhoneNumberNormalizer.normalize(incomingNumber!!)
                    val prefs = context.getSharedPreferences("cx_call_notes_prefs", Context.MODE_PRIVATE)
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = DatabaseProvider.get(context.applicationContext)
                        val contact = db.contactDao().findByPhone(phone)
                        val name = contact?.displayName ?: ""
                        prefs.edit().putString("last_call_phone", phone).putString("last_call_name", name).apply()
                        if (!name.startsWith("#")) {
                            Log.d("CXCalls", "PhoneStateReceiver: IDLE after call, launching PostCallNoteActivity")
                            try {
                                context.applicationContext.stopService(Intent(context.applicationContext, OverlayService::class.java))
                            } catch (_: Exception) {}
                            launchActivityWithNotification(context, phone, contact?.displayName, "incoming")
                        } else {
                            Log.d("CXCalls", "PhoneStateReceiver: Contact excluded (starts with #), skipping PostCallNoteActivity")
                        }
                    }
                } else if (lastState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val cursor = context.contentResolver.query(
                            android.provider.CallLog.Calls.CONTENT_URI,
                            arrayOf(android.provider.CallLog.Calls.NUMBER),
                            "${android.provider.CallLog.Calls.TYPE} = $OUTGOING_CALL_TYPE",
                            null,
                            "${android.provider.CallLog.Calls.DATE} DESC"
                        )
                        val phone = cursor?.use {
                            if (it.moveToFirst()) it.getString(0) else null
                        }
                        if (!phone.isNullOrBlank()) {
                            val normalizedPhone = PhoneNumberNormalizer.normalize(phone)
                            Log.d("CXCalls", "PhoneStateReceiver: IDLE after outgoing call, phone=$normalizedPhone")
                            val db = DatabaseProvider.get(context.applicationContext)
                            val contact = db.contactDao().findByPhone(normalizedPhone)
                            val name = contact?.displayName ?: ""
                            val prefs = context.getSharedPreferences("cx_call_notes_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("last_call_phone", normalizedPhone).putString("last_call_name", name).apply()
                            if (!name.startsWith("#")) {
                                launchActivityWithNotification(context, normalizedPhone, contact?.displayName, "outgoing")
                            }
                        }
                    }
                }
                wasRinging = false
                incomingNumber = null
                screenedNumber = null
                lastState = TelephonyManager.CALL_STATE_IDLE
            }
        }
    }
    private fun launchActivityWithNotification(context: Context, phone: String, contactName: String?, direction: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "CX Call Notes Alert", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Показва форма след разговор"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        val activityIntent = Intent(context.applicationContext, PostCallNoteActivity::class.java).apply {
            putExtra(PostCallNoteActivity.EXTRA_PHONE, phone)
            putExtra(PostCallNoteActivity.EXTRA_FROM_CALL, true)
            putExtra(PostCallNoteActivity.EXTRA_CALL_DIRECTION, direction)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        try {
            context.applicationContext.startActivity(activityIntent)
        } catch (e: Exception) {
            Log.e("CXCalls", "Direct startActivity failed: ${e.message}", e)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val displayName = if (!contactName.isNullOrBlank()) contactName else phone
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle("CX Call Notes")
            .setContentText("Добави бележка за $displayName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
