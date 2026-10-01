package com.agent.android.observation

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.agent.android.service.LocalAgentAccessibilityService
import java.util.concurrent.atomic.AtomicInteger

class AccessibilityObservationEngine(
    val snapshotStore: ObservationSnapshotStore = ObservationSnapshotStore()
) {

    companion object {
        private const val TAG = "AccessibilityObsEngine"
        const val MAX_NODE_LIMIT = 500
        const val MAX_DEPTH_LIMIT = 30
        const val DEBOUNCE_INTERVAL_MS = 1000L
    }

    val observationMode: ObservationMode
        get() = snapshotStore.observationMode

    @Volatile
    private var lastErrorText: String? = null

    @Volatile
    private var lastCaptureTimeMs: Long = 0L

    fun startObservationMode() {
        snapshotStore.startObservationMode()
    }

    fun stopObservationMode() {
        snapshotStore.stopObservationMode()
    }

    fun resetToReadyMode() {
        snapshotStore.resetToReadyMode()
    }

    fun getServiceInstance(): LocalAgentAccessibilityService? {
        return LocalAgentAccessibilityService.instance
    }

    fun isServiceConnected(): Boolean {
        return getServiceInstance() != null
    }

    fun isExcludedExternalPackage(pkgName: String?): Boolean {
        return snapshotStore.isExcludedExternalPackage(pkgName)
    }

    fun handleAccessibilityEvent(packageName: String?, eventType: Int) {
        if (observationMode != ObservationMode.OBSERVING) return

        val now = System.currentTimeMillis()
        if (now - lastCaptureTimeMs < DEBOUNCE_INTERVAL_MS) {
            return
        }

        val sessionToken = snapshotStore.getSessionId()
        lastCaptureTimeMs = now
        val snapshot = captureCurrentScreen()

        snapshotStore.updateFromCapture(snapshot, sessionToken)
        if (snapshot.state == ObservationState.SUCCESS && !isExcludedExternalPackage(snapshot.packageName)) {
            Log.i(TAG, "Captured external app observation: ${snapshot.packageName} (${snapshot.nodeCount} nodes)")
        }
    }

    fun getObservationMetadata(): ObservationMetadata {
        val service = getServiceInstance()
        val rootAvail = try {
            service?.rootInActiveWindow != null
        } catch (e: Exception) {
            false
        }

        val dispSnap = getDisplayedSnapshot()
        return ObservationMetadata(
            isAccessibilityInstalled = true,
            isAccessibilityEnabled = service != null,
            isServiceConnected = service != null,
            isRootAvailable = rootAvail,
            lastEventTimestampMs = service?.lastEventTimeMs,
            lastEventPackageName = service?.lastEventPackageName,
            lastEventType = service?.lastEventType,
            lastObservationTimestampMs = dispSnap?.timestampMs,
            lastObservationNodeCount = dispSnap?.nodeCount,
            lastError = lastErrorText ?: dispSnap?.error
        )
    }

    fun captureCurrentScreen(): ObservationSnapshot {
        val startMs = System.currentTimeMillis()
        val service = getServiceInstance()

        if (service == null) {
            val errSnapshot = ObservationSnapshot(
                timestampMs = startMs,
                packageName = "UNKNOWN",
                activityName = null,
                windowType = null,
                rootBounds = ObservationBounds(0, 0, 0, 0),
                nodeCount = 0,
                rootNode = null,
                allNodesList = emptyList(),
                state = ObservationState.ACCESSIBILITY_DISABLED,
                error = "LocalAgentAccessibilityService is not enabled or connected."
            )
            snapshotStore.updateFromCapture(errSnapshot, snapshotStore.getSessionId())
            lastErrorText = errSnapshot.error
            return errSnapshot
        }

        val rootNodeInfo: AccessibilityNodeInfo? = try {
            service.rootInActiveWindow
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching rootInActiveWindow: ${e.message}")
            null
        }

        if (rootNodeInfo == null) {
            val errSnapshot = ObservationSnapshot(
                timestampMs = startMs,
                packageName = "UNKNOWN",
                activityName = null,
                windowType = null,
                rootBounds = ObservationBounds(0, 0, 0, 0),
                nodeCount = 0,
                rootNode = null,
                allNodesList = emptyList(),
                state = ObservationState.ROOT_NODE_UNAVAILABLE,
                error = "Root Accessibility node is unavailable (No active window)."
            )
            snapshotStore.updateFromCapture(errSnapshot, snapshotStore.getSessionId())
            lastErrorText = errSnapshot.error
            return errSnapshot
        }

        val nodeCounter = AtomicInteger(0)
        val allNodesList = mutableListOf<ObservationNode>()

        val rootBoundsRect = Rect()
        try {
            rootNodeInfo.getBoundsInScreen(rootBoundsRect)
        } catch (ignored: Exception) {}

        val rootBounds = ObservationBounds(
            left = rootBoundsRect.left,
            top = rootBoundsRect.top,
            right = rootBoundsRect.right,
            bottom = rootBoundsRect.bottom
        )

        val pkgName = rootNodeInfo.packageName?.toString() ?: "UNKNOWN"
        val className = rootNodeInfo.className?.toString()

        val rootObservationNode = try {
            traverseNodeHierarchy(
                nodeInfo = rootNodeInfo,
                parentId = null,
                depth = 0,
                nodeCounter = nodeCounter,
                allNodesList = allNodesList
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error traversing accessibility tree: ${e.message}")
            null
        } finally {
            try {
                rootNodeInfo.recycle()
            } catch (ignored: Exception) {}
        }

        if (rootObservationNode == null) {
            val errSnapshot = ObservationSnapshot(
                timestampMs = startMs,
                packageName = pkgName,
                activityName = className,
                windowType = null,
                rootBounds = rootBounds,
                nodeCount = 0,
                rootNode = null,
                allNodesList = emptyList(),
                state = ObservationState.OBSERVATION_FAILED,
                error = "Failed to parse root node hierarchy."
            )
            snapshotStore.updateFromCapture(errSnapshot, snapshotStore.getSessionId())
            lastErrorText = errSnapshot.error
            return errSnapshot
        }

        val snapshot = ObservationSnapshot(
            timestampMs = startMs,
            packageName = pkgName,
            activityName = className,
            windowType = null,
            rootBounds = rootBounds,
            nodeCount = allNodesList.size,
            rootNode = rootObservationNode,
            allNodesList = allNodesList,
            state = ObservationState.SUCCESS,
            error = null
        )

        snapshotStore.updateFromCapture(snapshot, snapshotStore.getSessionId())
        lastErrorText = null
        return snapshot
    }

    private fun traverseNodeHierarchy(
        nodeInfo: AccessibilityNodeInfo,
        parentId: String?,
        depth: Int,
        nodeCounter: AtomicInteger,
        allNodesList: MutableList<ObservationNode>
    ): ObservationNode? {
        if (nodeCounter.get() >= MAX_NODE_LIMIT || depth > MAX_DEPTH_LIMIT) {
            return null
        }

        val currentIndex = nodeCounter.incrementAndGet()
        val nodeId = "node-$currentIndex"

        val rect = Rect()
        try {
            nodeInfo.getBoundsInScreen(rect)
        } catch (ignored: Exception) {}

        val bounds = ObservationBounds(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom
        )

        val childrenList = mutableListOf<ObservationNode>()
        val rawChildCount = try { nodeInfo.childCount } catch (e: Exception) { 0 }

        for (i in 0 until rawChildCount) {
            if (nodeCounter.get() >= MAX_NODE_LIMIT) break
            val childInfo: AccessibilityNodeInfo? = try {
                nodeInfo.getChild(i)
            } catch (e: Exception) {
                null
            }

            if (childInfo != null) {
                val childNode = traverseNodeHierarchy(
                    nodeInfo = childInfo,
                    parentId = nodeId,
                    depth = depth + 1,
                    nodeCounter = nodeCounter,
                    allNodesList = allNodesList
                )
                if (childNode != null) {
                    childrenList.add(childNode)
                }
                try {
                    childInfo.recycle()
                } catch (ignored: Exception) {}
            }
        }

        val obsNode = ObservationNode(
            id = nodeId,
            parentId = parentId,
            className = try { nodeInfo.className?.toString() } catch (e: Exception) { null },
            packageName = try { nodeInfo.packageName?.toString() } catch (e: Exception) { null },
            text = try { nodeInfo.text?.toString() } catch (e: Exception) { null },
            contentDescription = try { nodeInfo.contentDescription?.toString() } catch (e: Exception) { null },
            resourceId = try { nodeInfo.viewIdResourceName?.toString() } catch (e: Exception) { null },
            bounds = bounds,
            isClickable = try { nodeInfo.isClickable } catch (e: Exception) { false },
            isLongClickable = try { nodeInfo.isLongClickable } catch (e: Exception) { false },
            isFocusable = try { nodeInfo.isFocusable } catch (e: Exception) { false },
            isFocused = try { nodeInfo.isFocused } catch (e: Exception) { false },
            isEnabled = try { nodeInfo.isEnabled } catch (e: Exception) { true },
            isEditable = try { nodeInfo.isEditable } catch (e: Exception) { false },
            isScrollable = try { nodeInfo.isScrollable } catch (e: Exception) { false },
            isCheckable = try { nodeInfo.isCheckable } catch (e: Exception) { false },
            isChecked = try { nodeInfo.isChecked } catch (e: Exception) { false },
            isSelected = try { nodeInfo.isSelected } catch (e: Exception) { false },
            isVisibleToUser = try { nodeInfo.isVisibleToUser } catch (e: Exception) { true },
            isPassword = try { nodeInfo.isPassword } catch (e: Exception) { false },
            childCount = childrenList.size,
            children = childrenList
        )

        allNodesList.add(obsNode)
        return obsNode
    }

    fun getLastSnapshot(): ObservationSnapshot? = snapshotStore.displayedSnapshot ?: snapshotStore.currentLiveSnapshot
    fun getLastExternalSnapshot(): ObservationSnapshot? = snapshotStore.lastValidExternalSnapshot
    fun getDisplayedSnapshot(): ObservationSnapshot? = snapshotStore.displayedSnapshot

    fun clearLastSnapshot() {
        snapshotStore.clearSnapshots()
        lastErrorText = null
    }
}
