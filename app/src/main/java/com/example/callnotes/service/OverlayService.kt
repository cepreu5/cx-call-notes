package com.example.callnotes.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.example.callnotes.R

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    override fun onBind(intent: Intent?): IBinder? = null
    @SuppressLint("InflateParams")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CXCalls", "OverlayService onStartCommand")
        val name = intent?.getStringExtra(EXTRA_NAME) ?: "Unknown"
        val note = intent?.getStringExtra(EXTRA_NOTE) ?: ""
        Log.d("CXCalls", "OverlayService: name=$name, note=$note")
        showOverlay(name, note)
        return START_NOT_STICKY
    }
    @SuppressLint("InflateParams")
    private fun showOverlay(name: String, note: String) {
        Log.d("CXCalls", "showOverlay: canDrawOverlays=${android.provider.Settings.canDrawOverlays(this)}")
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Log.e("CXCalls", "OVERLAY PERMISSION NOT GRANTED - cannot show overlay")
            return
        }
        if (overlayView != null) {
            Log.d("CXCalls", "Removing existing overlay before adding new one")
            try { windowManager?.removeView(overlayView) } catch (_: Exception) {}
            overlayView = null
        }
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_note, null)
        val tvName = overlayView?.findViewById<TextView>(R.id.tv_caller_name)
        val tvNote = overlayView?.findViewById<TextView>(R.id.tv_caller_note)
        val btnClose = overlayView?.findViewById<Button>(R.id.btn_close_overlay)
        tvName?.text = name
        tvNote?.text = note.ifBlank { "Няма бележки за този контакт." }
        btnClose?.setOnClickListener { stopSelf() }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            y = 0
        }
        try {
            windowManager?.addView(overlayView, params)
            Log.d("CXCalls", "Overlay view ADDED successfully")
        } catch (e: Exception) {
            Log.e("CXCalls", "Failed to add overlay view", e)
        }
    }
    override fun onDestroy() {
        Log.d("CXCalls", "OverlayService onDestroy")
        super.onDestroy()
        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e("CXCalls", "Error removing overlay on destroy", e)
        }
    }
    companion object {
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_NOTE = "extra_note"
    }
}

