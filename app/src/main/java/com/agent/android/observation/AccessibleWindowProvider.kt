package com.agent.android.observation

import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.agent.android.service.LocalAgentAccessibilityService

class AccessibleWindowProvider {

    companion object {
        private const val TAG = "AccessibleWindowProv"
    }

    fun getServiceInstance(): LocalAgentAccessibilityService? {
        return LocalAgentAccessibilityService.instance
    }

    fun getActiveWindowRoot(): AccessibilityNodeInfo? {
        val service = getServiceInstance() ?: return null
        return try {
            service.rootInActiveWindow
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching rootInActiveWindow: ${e.message}")
            null
        }
    }

    fun getAccessibleWindows(): List<ObservedWindow> {
        val service = getServiceInstance() ?: return emptyList()
        val observedWindows = mutableListOf<ObservedWindow>()

        try {
            val windowList: List<AccessibilityWindowInfo>? = service.windows
            if (!windowList.isNullOrEmpty()) {
                for (win in windowList) {
                    val root: AccessibilityNodeInfo? = try {
                        win.root
                    } catch (e: Exception) {
                        null
                    }

                    val pkgName = root?.packageName?.toString() ?: "UNKNOWN"
                    val className = root?.className?.toString()

                    val rect = Rect()
                    try {
                        win.getBoundsInScreen(rect)
                    } catch (ignored: Exception) {}

                    val bounds = ObservationBounds(rect.left, rect.top, rect.right, rect.bottom)
                    val cls = WindowClassification.classify(pkgName, className, win.type)

                    val obsWin = ObservedWindow(
                        windowId = win.id,
                        packageName = pkgName,
                        activityName = className,
                        windowType = win.type,
                        layer = win.layer,
                        bounds = bounds,
                        isActive = win.isActive,
                        isFocused = win.isFocused,
                        isAccessibilityFocused = win.isAccessibilityFocused,
                        classification = cls,
                        rootNode = null
                    )
                    observedWindows.add(obsWin)

                    try {
                        root?.recycle()
                    } catch (ignored: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getWindows() query failed: ${e.message}")
        }

        // Fallback if service.windows returned empty or is unsupported on device
        if (observedWindows.isEmpty()) {
            val rootNode = getActiveWindowRoot()
            if (rootNode != null) {
                val pkgName = rootNode.packageName?.toString() ?: "UNKNOWN"
                val className = rootNode.className?.toString()
                val rect = Rect()
                try {
                    rootNode.getBoundsInScreen(rect)
                } catch (ignored: Exception) {}

                val cls = WindowClassification.classify(pkgName, className)
                val fallbackWin = ObservedWindow(
                    windowId = 0,
                    packageName = pkgName,
                    activityName = className,
                    windowType = 1, // TYPE_APPLICATION
                    layer = 0,
                    bounds = ObservationBounds(rect.left, rect.top, rect.right, rect.bottom),
                    isActive = true,
                    isFocused = true,
                    isAccessibilityFocused = true,
                    classification = cls
                )
                observedWindows.add(fallbackWin)
                try {
                    rootNode.recycle()
                } catch (ignored: Exception) {}
            }
        }

        return observedWindows
    }
}
