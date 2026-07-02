package com.example.callnotes.service

import android.content.Intent
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.callnotes.data.DatabaseProvider
import com.example.callnotes.data.PhoneNumberNormalizer
import com.example.callnotes.ui.PostCallNoteActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IncomingCallScreeningService : CallScreeningService() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var callStateWatcher: CallStateWatcher? = null
    override fun onScreenCall(details: Call.Details) {
        Log.d("CXCalls", "=== onScreenCall TRIGGERED ===")
        val raw = details.handle?.schemeSpecificPart
        Log.d("CXCalls", "Raw phone from handle: $raw")
        if (raw == null) {
            Log.e("CXCalls", "Handle schemeSpecificPart is NULL, returning early")
            return
        }
        val phone = PhoneNumberNormalizer.normalize(raw)
        Log.d("CXCalls", "Normalized phone: $phone")
        PhoneStateReceiver.screenedNumber = raw
        scope.launch {
            try {
                val db = DatabaseProvider.get(applicationContext)
                val contact = db.contactDao().findByPhone(phone)
                Log.d("CXCalls", "DB lookup result: contact=${contact?.displayName ?: "NULL"}, phone=$phone")
                respondToCall(
                    details,
                    CallResponse.Builder()
                        .setDisallowCall(false)
                        .setSilenceCall(false)
                        .setSkipCallLog(false)
                        .build()
                )
                Log.d("CXCalls", "respondToCall done")
                if (contact != null) {
                    if (contact.displayName.startsWith("#")) {
                        Log.d("CXCalls", "Contact excluded (starts with #): ${contact.displayName}")
                    } else {
                        Log.d("CXCalls", "KNOWN contact: name=${contact.displayName}, note=${contact.note}")
                        CallUiEvents.emitKnown(phone, contact.displayName, contact.note)
                        Log.d("CXCalls", "Checking overlay permission: ${android.provider.Settings.canDrawOverlays(applicationContext)}")
                        startOverlay(contact.displayName, contact.note ?: "", contact.tags ?: "")
                        Log.d("CXCalls", "startOverlay called")
                        startCallWatcher(phone)
                        Log.d("CXCalls", "startCallWatcher called")
                    }
                } else {
                    Log.d("CXCalls", "UNKNOWN contact for phone=$phone")
                    CallUiEvents.emitUnknown(phone)
                    startCallWatcher(phone)
                    Log.d("CXCalls", "startCallWatcher called")
                }
            } catch (e: Exception) {
                Log.e("CXCalls", "Exception in onScreenCall coroutine", e)
            }
        }
    }
    private fun startOverlay(name: String, note: String, tags: String) {
        Log.d("CXCalls", "startOverlay: name=$name, note=$note, tags=$tags")
        val intent = Intent(applicationContext, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_NAME, name)
            putExtra(OverlayService.EXTRA_NOTE, note)
            putExtra(OverlayService.EXTRA_TAGS, tags)
        }
        startService(intent)
        Log.d("CXCalls", "OverlayService startService called")
    }
    private fun startCallWatcher(phone: String) {
        scope.launch(Dispatchers.Main) {
            callStateWatcher?.stop()
            callStateWatcher = CallStateWatcher(applicationContext) {
                Log.d("CXCalls", "CallStateWatcher onIdle: stopping overlay, launching PostCallNoteActivity")
                stopService(Intent(applicationContext, OverlayService::class.java))
                val intent = Intent(applicationContext, PostCallNoteActivity::class.java).apply {
                    putExtra(PostCallNoteActivity.EXTRA_PHONE, phone)
                    putExtra(PostCallNoteActivity.EXTRA_FROM_CALL, true)
                    putExtra(PostCallNoteActivity.EXTRA_CALL_DIRECTION, "incoming")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
                callStateWatcher?.stop()
                callStateWatcher = null
            }
            callStateWatcher?.start()
            Log.d("CXCalls", "CallStateWatcher started")
        }
    }
    override fun onDestroy() {
        Log.d("CXCalls", "IncomingCallScreeningService onDestroy")
        callStateWatcher?.stop()
        super.onDestroy()
    }
}
