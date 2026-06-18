package com.example.callnotes.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
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
                                Log.d("CXCalls", "PhoneStateReceiver: KNOWN contact, starting overlay")
                                val overlayIntent = Intent(context.applicationContext, OverlayService::class.java).apply {
                                    putExtra(OverlayService.EXTRA_NAME, contact.displayName)
                                    putExtra(OverlayService.EXTRA_NOTE, contact.note ?: "")
                                    putExtra(OverlayService.EXTRA_TAGS, contact.tags ?: "")
                                }
                                context.applicationContext.startService(overlayIntent)
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
                    Log.d("CXCalls", "PhoneStateReceiver: IDLE after call, launching PostCallNoteActivity")
                    val phone = PhoneNumberNormalizer.normalize(incomingNumber!!)
                    val prefs = context.getSharedPreferences("cx_call_notes_prefs", Context.MODE_PRIVATE)
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = DatabaseProvider.get(context.applicationContext)
                        val contact = db.contactDao().findByPhone(phone)
                        val name = contact?.displayName ?: ""
                        prefs.edit().putString("last_call_phone", phone).putString("last_call_name", name).apply()
                    }
                    try {
                        context.applicationContext.stopService(Intent(context.applicationContext, OverlayService::class.java))
                    } catch (_: Exception) {}
                    val activityIntent = Intent(context.applicationContext, PostCallNoteActivity::class.java).apply {
                        putExtra(PostCallNoteActivity.EXTRA_PHONE, phone)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    context.applicationContext.startActivity(activityIntent)
                }
                wasRinging = false
                incomingNumber = null
                lastState = TelephonyManager.CALL_STATE_IDLE
            }
        }
    }
}
