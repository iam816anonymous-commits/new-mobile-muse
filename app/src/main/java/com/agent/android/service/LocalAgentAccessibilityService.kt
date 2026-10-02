package com.agent.android.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.agent.android.execution.ExecutionController
import com.agent.android.safety.CancellationReason

/**
 * Foundation Accessibility Service for LocalAgent.
 *
 * CRITICAL ARCHITECTURAL RULE:
 * onAccessibilityEvent() is OBSERVATION ONLY.
 *
 * It must NEVER:
 * - execute goals
 * - execute actions
 * - launch applications
 * - trigger planning
 * - start autonomous loops
 * - recursively crawl the accessibility tree
 * - dispatch gestures
 * - perform taps
 * - trigger recovery
 *
 * PHYSICAL VOLUME KEY PRESERVATION RULE:
 * Single physical volume key presses (KEYCODE_VOLUME_UP, KEYCODE_VOLUME_DOWN, KEYCODE_VOLUME_MUTE)
 * MUST return false immediately to preserve native Android volume key propagation.
 * Only a double Volume-Up press within 500ms is consumed (returns true) to trigger the emergency stop panic mechanism.
 */
class LocalAgentAccessibilityService : AccessibilityService() {

    @Volatile
    private var lastVolumeUpTimeMs: Long = 0L

    var executionController: ExecutionController? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        try {
            Log.i(TAG, "LocalAgentAccessibilityService connected")
        } catch (ignored: Throwable) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // OBSERVATION ONLY.
        // Do not execute any actions or trigger autonomous loops from here.
    }

    fun handleKeyEventInternal(keyCode: Int, action: Int, eventTimeMs: Long): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && action == KeyEvent.ACTION_DOWN) {
            val nowMs = if (eventTimeMs > 0) eventTimeMs else System.currentTimeMillis()
            if (lastVolumeUpTimeMs > 0L && (nowMs - lastVolumeUpTimeMs) <= PANIC_THRESHOLD_MS) {
                lastVolumeUpTimeMs = 0L
                triggerPanicEmergencyStop()
                return true
            } else {
                lastVolumeUpTimeMs = nowMs
            }
        }
        return false
    }

    public override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event != null) {
            val handled = handleKeyEventInternal(
                event.keyCode,
                event.action,
                if (event.eventTime > 0) event.eventTime else System.currentTimeMillis()
            )
            if (handled) return true
        }
        return super.onKeyEvent(event)
    }

    private fun triggerPanicEmergencyStop() {
        try {
            Log.e(TAG, "HARDWARE PANIC BUTTON DETECTED (Double Volume-Up). Triggering Emergency Stop!")
        } catch (ignored: Throwable) {}

        executionController?.let { controller ->
            controller.cancellationManager.requestCancellation(CancellationReason.USER_PANIC)
            controller.getActiveJob()?.cancel()
            controller.releaseExecution(CancellationReason.USER_PANIC)
        }
        try {
            performGlobalAction(GLOBAL_ACTION_HOME)
        } catch (ignored: Throwable) {}
    }

    override fun onInterrupt() {
        try {
            Log.w(TAG, "LocalAgentAccessibilityService interrupted")
        } catch (ignored: Throwable) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
        try {
            Log.i(TAG, "LocalAgentAccessibilityService destroyed")
        } catch (ignored: Throwable) {}
    }

    companion object {
        private const val TAG = "LocalAgentAccService"
        const val PANIC_THRESHOLD_MS = 500L

        @Volatile
        @JvmStatic
        var instance: LocalAgentAccessibilityService? = null
            private set
    }
}
