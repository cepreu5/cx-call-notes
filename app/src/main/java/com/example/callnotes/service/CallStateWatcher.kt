package com.example.callnotes.service

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.callnotes.ui.PostCallNoteActivity
import java.util.concurrent.Executor

class CallStateWatcher(
    private val context: Context,
    private val onIdle: () -> Unit
) {
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val executor = Executor { it.run() }
    private var wasInCall = false
    private val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> wasInCall = true
                TelephonyManager.CALL_STATE_OFFHOOK -> wasInCall = true
                TelephonyManager.CALL_STATE_IDLE -> {
                    if (wasInCall) {
                        wasInCall = false
                        onIdle()
                    }
                }
            }
        }
    }
    fun start() {
        telephonyManager.registerTelephonyCallback(executor, callback)
    }
    fun stop() {
        telephonyManager.unregisterTelephonyCallback(callback)
    }
}
