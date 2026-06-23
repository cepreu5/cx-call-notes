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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import com.example.callnotes.R

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("InflateParams")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CXCalls", "OverlayService onStartCommand")
        val name = intent?.getStringExtra(EXTRA_NAME) ?: "Unknown"
        val note = intent?.getStringExtra(EXTRA_NOTE) ?: ""
        val tags = intent?.getStringExtra(EXTRA_TAGS) ?: ""
        val displayNote = com.example.callnotes.ui.PostCallNoteUiState.stripDirectionPrefix(note)
        Log.d("CXCalls", "OverlayService: name=$name, note=$displayNote, tags=$tags")
        showOverlay(name, displayNote, tags)
        return START_NOT_STICKY
    }

    @SuppressLint("InflateParams")
    private fun showOverlay(name: String, note: String, tags: String) {
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
        val tvTags = overlayView?.findViewById<TextView>(R.id.tv_caller_tags)
        val btnClose = overlayView?.findViewById<ImageButton>(R.id.btn_close_overlay)
        tvName?.text = name
        tvNote?.text = note.ifBlank { "Няма бележки за този контакт." }
        if (tags.isNotBlank()) {
            tvTags?.visibility = View.VISIBLE
            tvTags?.text = "Етикети: $tags"
        } else {
            tvTags?.visibility = View.GONE
        }
        btnClose?.setOnClickListener { stopSelf() }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedX = prefs.getInt(KEY_X, Int.MIN_VALUE)
        val savedY = prefs.getInt(KEY_Y, Int.MIN_VALUE)

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = wm.currentWindowMetrics
        val screenWidth = metrics.bounds.width()
        val screenHeight = metrics.bounds.height()

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (savedX != Int.MIN_VALUE && savedY != Int.MIN_VALUE) {
                x = savedX
                y = savedY
                Log.d("CXCalls", "Loaded saved position: x=$savedX, y=$savedY")
            } else {
                x = (screenWidth - 500) / 2
                y = 200
                Log.d("CXCalls", "Using default position: x=$x, y=$y")
            }
        }

        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params!!.x
                    initialY = params!!.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (!isDragging && (dx * dx + dy * dy > DRAG_THRESHOLD * DRAG_THRESHOLD)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params!!.x = initialX + dx.toInt()
                        params!!.y = initialY + dy.toInt()
                        windowManager?.updateViewLayout(overlayView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        prefs.edit()
                            .putInt(KEY_X, params!!.x)
                            .putInt(KEY_Y, params!!.y)
                            .apply()
                        Log.d("CXCalls", "Overlay position saved: x=${params!!.x}, y=${params!!.y}")
                    }
                    true
                }
                else -> false
            }
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
        const val EXTRA_TAGS = "extra_tags"
        private const val PREFS_NAME = "overlay_position"
        private const val KEY_X = "overlay_x"
        private const val KEY_Y = "overlay_y"
        private const val DRAG_THRESHOLD = 10
    }
}
