package com.agent.android.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

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
 */
class LocalAgentAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "LocalAgentAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // OBSERVATION ONLY.
        // Do not execute any actions or trigger autonomous loops from here.
    }

    override fun onInterrupt() {
        Log.w(TAG, "LocalAgentAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "LocalAgentAccessibilityService destroyed")
    }

    companion object {
        private const val TAG = "LocalAgentAccService"
    }
}
