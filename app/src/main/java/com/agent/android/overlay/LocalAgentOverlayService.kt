package com.agent.android.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.agent.android.execution.GoalDispatcherImpl

class LocalAgentOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayContainer: LinearLayout? = null
    private var collapsedView: TextView? = null
    private var expandedPanel: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var isExpanded: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        setupOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_HIDE) {
            hideOverlay()
        } else if (action == ACTION_SHOW) {
            showOverlay()
        }
        return START_STICKY
    }

    private fun setupOverlayView() {
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 50
                y = 200
            }

            overlayContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 8, 8, 8)
                setBackgroundColor(Color.TRANSPARENT)
            }

            collapsedView = TextView(this).apply {
                text = " [LA] "
                setTextColor(Color.CYAN)
                textSize = 16f
                setPadding(16, 12, 16, 12)
                setBackgroundColor(Color.argb(220, 10, 15, 30))
            }

            expandedPanel = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.argb(240, 15, 20, 35))
                setPadding(12, 12, 12, 12)
                visibility = View.GONE
            }

            val actions = listOf(
                "BACK" to "back",
                "HOME" to "home",
                "RECENTS" to "recents",
                "SCROLL UP" to "scroll backward",
                "SCROLL DOWN" to "scroll forward",
                "CLICK" to "click target",
                "LONG CLICK" to "long click target",
                "OBSERVE" to "observe current",
                "STATUS" to "action status",
                "HIDE" to "HIDE"
            )

            for ((label, cmd) in actions) {
                val btn = Button(this).apply {
                    text = label
                    textSize = 12f
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.argb(180, 30, 40, 60))
                    setOnClickListener {
                        if (label == "HIDE") {
                            toggleExpand(false)
                        } else {
                            dispatchOverlayAction(cmd)
                        }
                    }
                }
                expandedPanel?.addView(btn)
            }

            collapsedView?.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params?.x ?: 0
                        initialY = params?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params?.x = initialX + (event.rawX - initialTouchX).toInt()
                        params?.y = initialY + (event.rawY - initialTouchY).toInt()
                        params?.let { windowManager?.updateViewLayout(overlayContainer, it) }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)
                        if (diffX < 10 && diffY < 10) {
                            toggleExpand(!isExpanded)
                        }
                        true
                    }
                    else -> false
                }
            }

            overlayContainer?.addView(collapsedView)
            overlayContainer?.addView(expandedPanel)

            windowManager?.addView(overlayContainer, params)
            isOverlayVisible = true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing overlay view: ${e.message}")
        }
    }

    private fun toggleExpand(expand: Boolean) {
        isExpanded = expand
        expandedPanel?.visibility = if (expand) View.VISIBLE else View.GONE
    }

    fun dispatchOverlayAction(command: String) {
        val dispatcher = goalDispatcher ?: GoalDispatcherImpl.instance
        if (dispatcher != null) {
            dispatcher.dispatchAndProcessWithLock(command)
        } else {
            Log.w(TAG, "GoalDispatcher instance unavailable for overlay command '$command'")
        }
    }

    fun showOverlay() {
        overlayContainer?.visibility = View.VISIBLE
        isOverlayVisible = true
    }

    fun hideOverlay() {
        overlayContainer?.visibility = View.GONE
        isOverlayVisible = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayContainer != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayContainer)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view on destroy: ${e.message}")
            }
        }
        isOverlayVisible = false
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        private const val TAG = "LocalAgentOverlay"
        const val ACTION_SHOW = "com.agent.android.overlay.SHOW"
        const val ACTION_HIDE = "com.agent.android.overlay.HIDE"

        @Volatile
        var instance: LocalAgentOverlayService? = null
            private set

        @Volatile
        var isOverlayVisible: Boolean = false
            private set

        @Volatile
        var goalDispatcher: GoalDispatcherImpl? = null
    }
}
