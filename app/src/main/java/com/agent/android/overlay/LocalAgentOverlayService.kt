package com.agent.android.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
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
        Log.i(TAG, "[Overlay] OVERLAY_SERVICE_CREATED")
        setupOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "[Overlay] OVERLAY_SERVICE_STARTED (action=${intent?.action})")
        val action = intent?.action
        if (action == ACTION_HIDE) {
            hideOverlay()
        } else if (action == ACTION_SHOW) {
            showOverlay()
        }
        return START_STICKY
    }

    private fun setupOverlayView() {
        Log.i(TAG, "[Overlay] OVERLAY_SHOW_REQUEST")

        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "[Overlay] OVERLAY_PERMISSION_STATE: DENIED (canDrawOverlays = false)")
            lastError = "OVERLAY_PERMISSION_REQUIRED: Display over other apps permission not granted"
            return
        }
        Log.i(TAG, "[Overlay] OVERLAY_PERMISSION_STATE: GRANTED")

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (windowManager == null) {
                Log.e(TAG, "[Overlay] OVERLAY_ADD_VIEW_FAILURE: WindowManager unavailable")
                lastError = "WindowManager service unavailable"
                return
            }

            // Android 8.0+ (API 26+) requires TYPE_APPLICATION_OVERLAY for non-system drawing over other apps.
            // On API <26, TYPE_PHONE is used for legacy compatibility.
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // Safe initial position calculation using screen bounds
            val display = windowManager?.defaultDisplay
            val metrics = DisplayMetrics()
            display?.getRealMetrics(metrics)
            val screenWidth = if (metrics.widthPixels > 0) metrics.widthPixels else 1080
            val screenHeight = if (metrics.heightPixels > 0) metrics.heightPixels else 1920

            val safeX = (screenWidth - 150).coerceAtLeast(0)
            val safeY = (screenHeight / 2 - 100).coerceAtLeast(0)

            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = safeX
                y = safeY
            }
            Log.i(TAG, "[Overlay] OVERLAY_LAYOUT_CREATED (type=$layoutType, initPos=($safeX,$safeY))")

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
                "BACK" to "action.back",
                "HOME" to "action.home",
                "RECENTS" to "action.recents",
                "SCROLL UP" to "action.scroll backward",
                "SCROLL DOWN" to "action.scroll forward",
                "CLICK" to "action.click target",
                "LONG CLICK" to "action.long_click target",
                "OBSERVE" to "observe.current",
                "STATUS" to "action.status",
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
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()

                        val newX = (initialX + dx).coerceIn(0, (screenWidth - 100).coerceAtLeast(0))
                        val newY = (initialY + dy).coerceIn(0, (screenHeight - 100).coerceAtLeast(0))

                        params?.x = newX
                        params?.y = newY
                        params?.let {
                            if (overlayContainer != null && isViewAttached) {
                                windowManager?.updateViewLayout(overlayContainer, it)
                            }
                        }
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
            Log.i(TAG, "[Overlay] OVERLAY_VIEW_CREATED")

            // Duplicate attachment protection
            if (isViewAttached) {
                Log.w(TAG, "[Overlay] OVERLAY_ALREADY_VISIBLE - addView skipped")
                isOverlayVisible = true
                return
            }

            Log.i(TAG, "[Overlay] OVERLAY_ADD_VIEW_ATTEMPT")
            windowManager?.addView(overlayContainer, params)
            isViewAttached = true
            isOverlayVisible = true
            lastError = null
            Log.i(TAG, "[Overlay] OVERLAY_ADD_VIEW_SUCCESS (attached=true, visibility=VISIBLE)")

            // Post addView verification
            val isAttached = overlayContainer?.isAttachedToWindow == true || overlayContainer?.windowToken != null
            if (!isAttached) {
                Log.w(TAG, "[Overlay] OVERLAY_NOT_ATTACHED after addView")
            }
        } catch (e: Exception) {
            isViewAttached = false
            isOverlayVisible = false
            lastError = "OVERLAY_ADD_VIEW_FAILURE: ${e.javaClass.simpleName} - ${e.message}"
            Log.e(TAG, "[Overlay] $lastError", e)
        }
    }

    private fun toggleExpand(expand: Boolean) {
        isExpanded = expand
        expandedPanel?.visibility = if (expand) View.VISIBLE else View.GONE
        Log.i(TAG, "[Overlay] Panel toggled expanded=$expand")
    }

    fun dispatchOverlayAction(command: String) {
        val dispatcher = goalDispatcher ?: GoalDispatcherImpl.instance
        if (dispatcher != null) {
            Log.i(TAG, "[Overlay] Dispatching overlay action: '$command'")
            dispatcher.dispatchAndProcessWithLock(command)
        } else {
            Log.w(TAG, "[Overlay] GoalDispatcher instance unavailable for overlay command '$command'")
        }
    }

    fun showOverlay() {
        if (!isViewAttached) {
            setupOverlayView()
        } else {
            overlayContainer?.visibility = View.VISIBLE
            isOverlayVisible = true
            Log.i(TAG, "[Overlay] showOverlay - set VISIBLE")
        }
    }

    fun hideOverlay() {
        overlayContainer?.visibility = View.GONE
        isOverlayVisible = false
        Log.i(TAG, "[Overlay] hideOverlay - set GONE")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "[Overlay] OVERLAY_SERVICE_DESTROYED")
        if (overlayContainer != null && windowManager != null && isViewAttached) {
            try {
                windowManager?.removeView(overlayContainer)
            } catch (e: Exception) {
                Log.e(TAG, "[Overlay] Error removing overlay view on destroy: ${e.message}")
            }
        }
        isViewAttached = false
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
        var isViewAttached: Boolean = false
            private set

        @Volatile
        var lastError: String? = null
            private set

        @Volatile
        var goalDispatcher: GoalDispatcherImpl? = null

        fun checkOverlayPermission(context: Context): Boolean {
            return Settings.canDrawOverlays(context)
        }

        fun getManageOverlayPermissionIntent(context: Context): Intent {
            return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        fun getDiagnosticStatus(context: Context?): String {
            val inst = instance
            val permGranted = context?.let { checkOverlayPermission(it) } ?: false
            val runningState = if (inst != null) "RUNNING" else "STOPPED"
            val permState = if (permGranted) "GRANTED" else "DENIED"
            val attachState = if (isViewAttached) "ATTACHED" else "DETACHED"
            val visState = if (isOverlayVisible) "VISIBLE" else "HIDDEN"
            val wmState = if (inst?.windowManager != null) "AVAILABLE" else "UNAVAILABLE"

            val posX = inst?.params?.x ?: -1
            val posY = inst?.params?.y ?: -1
            val width = inst?.overlayContainer?.width ?: -1
            val height = inst?.overlayContainer?.height ?: -1
            val err = lastError ?: "NONE"

            return """
                Movable Action Overlay Subsystem Diagnostics:
                Overlay Service: $runningState
                Overlay Permission: $permState
                Overlay View: $attachState
                Overlay Visibility: $visState
                WindowManager: $wmState
                Position: ($posX, $posY)
                Size: ${width}x${height}
                Last Error: $err
            """.trimIndent()
        }
    }
}
