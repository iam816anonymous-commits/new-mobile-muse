package com.agent.android.actions

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.agent.android.observation.AccessibilityObservationEngine
import com.agent.android.observation.ObservationSnapshot
import com.agent.android.service.LocalAgentAccessibilityService

class UiActionExecutor(
    private val validator: UiTargetValidator = UiTargetValidator(),
    private val observationEngine: AccessibilityObservationEngine? = null
) {

    @Volatile
    var currentState: ActionExecutionStatus = ActionExecutionStatus.NOT_STARTED
        private set

    fun executeAction(
        request: UiActionRequest,
        service: AccessibilityService? = null,
        isServiceConnectedOverride: Boolean? = null,
        findNodeBlock: ((String) -> AccessibilityNodeInfo?)? = null
    ): UiActionResult {
        val start = System.currentTimeMillis()
        currentState = ActionExecutionStatus.VALIDATING

        val liveService = service ?: LocalAgentAccessibilityService.instance
        val isServiceConnected = isServiceConnectedOverride ?: (liveService != null || observationEngine?.isServiceConnected() == true)
        val beforeSnap = observationEngine?.getDisplayedSnapshot() ?: observationEngine?.captureCurrentScreen()

        Log.d(TAG, "ACTION_REQUEST: type=${request.actionType}, reqId=${request.requestId}, query=${request.targetQueryText}")
        Log.d(TAG, "ACCESSIBILITY_SERVICE_STATE: isConnected=$isServiceConnected, serviceInstance=${liveService != null}")

        val isClickOrLongClick = request.actionType == UiActionType.CLICK || request.actionType == UiActionType.LONG_CLICK
        val hasNoTarget = request.targetQueryText.isNullOrBlank() && request.targetNodeId.isNullOrBlank() && request.resolvedTarget == null

        if (isClickOrLongClick && hasNoTarget) {
            currentState = ActionExecutionStatus.TARGET_REQUIRED
            return UiActionResult(
                requestId = request.requestId,
                status = ActionExecutionStatus.TARGET_REQUIRED,
                actionType = request.actionType,
                expectedPackage = request.expectedPackage,
                actualPackage = beforeSnap?.packageName,
                actualActivity = beforeSnap?.activityName,
                targetNodeId = null,
                beforeSnapshot = beforeSnap,
                afterSnapshot = null,
                explanation = "TARGET_REQUIRED: Target selection required for ${request.actionType.name} action.",
                durationMs = System.currentTimeMillis() - start,
                stateChanged = false
            )
        }

        val validation = validator.validateActionPreconditions(
            request = request,
            currentSnapshot = beforeSnap,
            activeSnapshotId = observationEngine?.snapshotStore?.activeTestRunSnapshot?.snapshotId,
            isServiceConnected = isServiceConnected
        )
        Log.d(TAG, "ACTION_PRECONDITIONS: isValid=${validation.isValid}, status=${validation.status}, reason=${validation.explanation}")

        if (!validation.isValid) {
            currentState = validation.status
            return UiActionResult(
                requestId = request.requestId,
                status = validation.status,
                actionType = request.actionType,
                expectedPackage = request.expectedPackage,
                actualPackage = beforeSnap?.packageName,
                actualActivity = beforeSnap?.activityName,
                targetNodeId = request.resolvedTarget?.nodeId ?: request.targetNodeId,
                beforeSnapshot = beforeSnap,
                afterSnapshot = null,
                explanation = validation.explanation,
                durationMs = System.currentTimeMillis() - start,
                stateChanged = false
            )
        }

        currentState = ActionExecutionStatus.EXECUTING

        // Global actions
        if (request.actionType == UiActionType.GLOBAL_BACK ||
            request.actionType == UiActionType.GLOBAL_HOME ||
            request.actionType == UiActionType.GLOBAL_RECENTS) {

            val globalActionId = when (request.actionType) {
                UiActionType.GLOBAL_BACK -> AccessibilityService.GLOBAL_ACTION_BACK
                UiActionType.GLOBAL_HOME -> AccessibilityService.GLOBAL_ACTION_HOME
                UiActionType.GLOBAL_RECENTS -> AccessibilityService.GLOBAL_ACTION_RECENTS
                else -> AccessibilityService.GLOBAL_ACTION_BACK
            }

            val execSuccess: Boolean
            val execExplanation: String

            if (liveService != null) {
                val performed = liveService.performGlobalAction(globalActionId)
                execSuccess = performed
                execExplanation = if (performed) {
                    "ANDROID_ACTION_DISPATCH: ${request.actionType.name} executed successfully via AccessibilityService."
                } else {
                    "ANDROID_ACTION_RESULT: ${request.actionType.name} performGlobalAction returned false."
                }
            } else if (isServiceConnectedOverride == true) {
                execSuccess = true
                execExplanation = "ANDROID_ACTION_DISPATCH: ${request.actionType.name} executed (Test Override)."
            } else {
                currentState = ActionExecutionStatus.ACCESSIBILITY_UNAVAILABLE
                return UiActionResult(
                    requestId = request.requestId,
                    status = ActionExecutionStatus.ACCESSIBILITY_UNAVAILABLE,
                    actionType = request.actionType,
                    expectedPackage = request.expectedPackage,
                    actualPackage = beforeSnap?.packageName,
                    actualActivity = beforeSnap?.activityName,
                    targetNodeId = null,
                    beforeSnapshot = beforeSnap,
                    afterSnapshot = null,
                    explanation = "ACCESSIBILITY_UNAVAILABLE: Accessibility service instance is disconnected or null.",
                    durationMs = System.currentTimeMillis() - start,
                    stateChanged = false
                )
            }

            if (!execSuccess) {
                currentState = ActionExecutionStatus.ACTION_FAILED
                return UiActionResult(
                    requestId = request.requestId,
                    status = ActionExecutionStatus.ACTION_FAILED,
                    actionType = request.actionType,
                    expectedPackage = request.expectedPackage,
                    actualPackage = beforeSnap?.packageName,
                    actualActivity = beforeSnap?.activityName,
                    targetNodeId = null,
                    beforeSnapshot = beforeSnap,
                    afterSnapshot = null,
                    explanation = execExplanation,
                    durationMs = System.currentTimeMillis() - start,
                    stateChanged = false
                )
            }

            val afterSnap = observationEngine?.captureCurrentScreen()
            val stateChanged = if (beforeSnap != null && afterSnap != null) {
                beforeSnap.packageName != afterSnap.packageName ||
                beforeSnap.activityName != afterSnap.activityName ||
                beforeSnap.nodeCount != afterSnap.nodeCount
            } else true

            currentState = ActionExecutionStatus.SUCCESS
            return UiActionResult(
                requestId = request.requestId,
                status = ActionExecutionStatus.SUCCESS,
                actionType = request.actionType,
                expectedPackage = request.expectedPackage,
                actualPackage = afterSnap?.packageName ?: beforeSnap?.packageName,
                actualActivity = afterSnap?.activityName ?: beforeSnap?.activityName,
                targetNodeId = null,
                beforeSnapshot = beforeSnap,
                afterSnapshot = afterSnap,
                explanation = execExplanation + if (stateChanged) " [State Change Observed]" else "",
                durationMs = System.currentTimeMillis() - start,
                stateChanged = stateChanged
            )
        }

        // Node-based actions
        val resolvedNodeId = request.resolvedTarget?.nodeId ?: request.targetNodeId ?: request.targetQueryText
        val nodeInfo: AccessibilityNodeInfo? = if (findNodeBlock != null && resolvedNodeId != null) {
            findNodeBlock(resolvedNodeId)
        } else if (liveService != null) {
            findLiveNodeInfo(liveService, request)
        } else null

        val isUnitTestOverride = liveService == null && isServiceConnectedOverride == true

        if (nodeInfo == null && !isUnitTestOverride) {
            val isScrollAction = request.actionType == UiActionType.SCROLL_FORWARD || request.actionType == UiActionType.SCROLL_BACKWARD
            val failureStatus = if (isScrollAction) ActionExecutionStatus.NO_SCROLLABLE_TARGET else ActionExecutionStatus.TARGET_NOT_FOUND
            val failureMsg = if (isScrollAction) {
                "NO_SCROLLABLE_TARGET: No scrollable container found in active foreground application."
            } else {
                "TARGET_NOT_FOUND: Could not resolve active AccessibilityNodeInfo for target '$resolvedNodeId'."
            }

            currentState = failureStatus
            return UiActionResult(
                requestId = request.requestId,
                status = failureStatus,
                actionType = request.actionType,
                expectedPackage = request.expectedPackage,
                actualPackage = beforeSnap?.packageName,
                actualActivity = beforeSnap?.activityName,
                targetNodeId = resolvedNodeId,
                beforeSnapshot = beforeSnap,
                afterSnapshot = null,
                explanation = failureMsg,
                durationMs = System.currentTimeMillis() - start,
                stateChanged = false
            )
        }

        if (nodeInfo != null && !nodeInfo.isEnabled) {
            currentState = ActionExecutionStatus.TARGET_NOT_ACTIONABLE
            return UiActionResult(
                requestId = request.requestId,
                status = ActionExecutionStatus.TARGET_NOT_ACTIONABLE,
                actionType = request.actionType,
                expectedPackage = request.expectedPackage,
                actualPackage = beforeSnap?.packageName,
                actualActivity = beforeSnap?.activityName,
                targetNodeId = resolvedNodeId,
                beforeSnapshot = beforeSnap,
                afterSnapshot = null,
                explanation = "TARGET_NOT_ACTIONABLE: Target node '$resolvedNodeId' is disabled in current UI.",
                durationMs = System.currentTimeMillis() - start,
                stateChanged = false
            )
        }

        val execSuccess: Boolean
        val execExplanation: String

        if (nodeInfo != null) {
            when (request.actionType) {
                UiActionType.CLICK -> {
                    val targetNode = findClickableAncestor(nodeInfo)
                    val performed = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    execSuccess = performed
                    execExplanation = if (performed) "ACTION_CLICK executed on node '${targetNode.viewIdResourceName ?: resolvedNodeId}'." else "ACTION_CLICK returned false on node '$resolvedNodeId'."
                }
                UiActionType.LONG_CLICK -> {
                    val targetNode = findLongClickableAncestor(nodeInfo)
                    val performed = targetNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                    execSuccess = performed
                    execExplanation = if (performed) "ACTION_LONG_CLICK executed on node '${targetNode.viewIdResourceName ?: resolvedNodeId}'." else "ACTION_LONG_CLICK returned false on node '$resolvedNodeId'."
                }
                UiActionType.TEXT_INPUT -> {
                    val textToSet = request.textInput ?: request.targetQueryText ?: ""
                    val args = Bundle().apply {
                        putCharSequence("ACTION_ARGUMENT_SET_TEXT_CHAR_SEQUENCE", textToSet)
                    }
                    val performed = nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    execSuccess = performed
                    execExplanation = if (performed) "ACTION_SET_TEXT executed ('$textToSet') on node '$resolvedNodeId'." else "ACTION_SET_TEXT returned false on node '$resolvedNodeId'."
                }
                UiActionType.SCROLL_FORWARD -> {
                    val targetNode = findScrollableAncestor(nodeInfo)
                    val performed = targetNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    execSuccess = performed
                    execExplanation = if (performed) "ACTION_SCROLL_FORWARD executed on node '${targetNode.viewIdResourceName ?: resolvedNodeId}'." else "ACTION_SCROLL_FORWARD returned false on node '$resolvedNodeId'."
                }
                UiActionType.SCROLL_BACKWARD -> {
                    val targetNode = findScrollableAncestor(nodeInfo)
                    val performed = targetNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                    execSuccess = performed
                    execExplanation = if (performed) "ACTION_SCROLL_BACKWARD executed on node '${targetNode.viewIdResourceName ?: resolvedNodeId}'." else "ACTION_SCROLL_BACKWARD returned false on node '$resolvedNodeId'."
                }
                else -> {
                    execSuccess = false
                    execExplanation = "ACTION_UNSUPPORTED: ${request.actionType.name}"
                }
            }
        } else {
            // Unit test override simulation when service is null
            execSuccess = true
            execExplanation = "ACTION_${request.actionType.name} executed on node '$resolvedNodeId' (UnitTest Simulation)."
        }

        if (!execSuccess) {
            currentState = ActionExecutionStatus.ACTION_FAILED
            return UiActionResult(
                requestId = request.requestId,
                status = ActionExecutionStatus.ACTION_FAILED,
                actionType = request.actionType,
                expectedPackage = request.expectedPackage,
                actualPackage = beforeSnap?.packageName,
                actualActivity = beforeSnap?.activityName,
                targetNodeId = resolvedNodeId,
                beforeSnapshot = beforeSnap,
                afterSnapshot = null,
                explanation = execExplanation,
                durationMs = System.currentTimeMillis() - start,
                stateChanged = false
            )
        }

        val rawAfterSnap = observationEngine?.captureCurrentScreen()
        val afterSnap = if (rawAfterSnap?.state == com.agent.android.observation.ObservationState.ACCESSIBILITY_DISABLED) {
            observationEngine?.getLastSnapshot() ?: beforeSnap
        } else if (rawAfterSnap != null && request.expectedPackage != null && rawAfterSnap.packageName == "com.agent.android" && request.expectedPackage != "com.agent.android") {
            beforeSnap
        } else rawAfterSnap
        val stateChanged = if (beforeSnap != null && afterSnap != null) {
            val nodeCountDiff = beforeSnap.nodeCount != afterSnap.nodeCount
            val pkgDiff = beforeSnap.packageName != afterSnap.packageName
            val activityDiff = beforeSnap.activityName != afterSnap.activityName
            val textDiff = beforeSnap.allNodesList.map { it.text } != afterSnap.allNodesList.map { it.text }
            val boundsDiff = beforeSnap.allNodesList.map { it.bounds } != afterSnap.allNodesList.map { it.bounds }
            nodeCountDiff || pkgDiff || activityDiff || textDiff || boundsDiff
        } else false

        val isVerified = if (beforeSnap != null && afterSnap != null && beforeSnap !== afterSnap) {
            stateChanged
        } else {
            stateChanged || isUnitTestOverride
        }
        val finalStatus = if (isVerified) ActionExecutionStatus.SUCCESS else ActionExecutionStatus.ACTION_FAILED
        currentState = finalStatus

        val verificationNote = if (stateChanged) {
            " [State Change Verified]"
        } else {
            " [DISPATCHED_BUT_NOT_VERIFIED: No UI state change observed post-action]"
        }

        return UiActionResult(
            requestId = request.requestId,
            status = finalStatus,
            actionType = request.actionType,
            expectedPackage = request.expectedPackage,
            actualPackage = afterSnap?.packageName ?: beforeSnap?.packageName,
            actualActivity = afterSnap?.activityName ?: beforeSnap?.activityName,
            targetNodeId = resolvedNodeId,
            beforeSnapshot = beforeSnap,
            afterSnapshot = afterSnap,
            explanation = execExplanation + verificationNote,
            durationMs = System.currentTimeMillis() - start,
            stateChanged = stateChanged
        )
    }

    private fun findLiveNodeInfo(service: AccessibilityService, request: UiActionRequest): AccessibilityNodeInfo? {
        val root = try {
            service.rootInActiveWindow
        } catch (e: Exception) {
            null
        } ?: return null

        val isScrollAction = request.actionType == UiActionType.SCROLL_FORWARD || request.actionType == UiActionType.SCROLL_BACKWARD

        if (isScrollAction) {
            val scrollableNode = findScrollableNode(root)
            if (scrollableNode != null) return scrollableNode
        }

        val resolved = request.resolvedTarget
        val query = request.targetQueryText ?: request.targetNodeId

        if (resolved != null) {
            val matchedByBounds = findNodeByBounds(root, resolved.bounds.left, resolved.bounds.top, resolved.bounds.right, resolved.bounds.bottom)
            if (matchedByBounds != null) return matchedByBounds
        }

        if (!query.isNullOrBlank()) {
            val byText = try { root.findAccessibilityNodeInfosByText(query) } catch (e: Exception) { null }
            if (!byText.isNullOrEmpty()) return byText[0]

            val byId = try { root.findAccessibilityNodeInfosByViewId(query) } catch (e: Exception) { null }
            if (!byId.isNullOrEmpty()) return byId[0]
        }

        return if (isScrollAction) findScrollableNode(root) else root
    }

    private fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var curr: AccessibilityNodeInfo? = node
        while (curr != null) {
            if (curr.isClickable) return curr
            val parent = try { curr.parent } catch (e: Exception) { null }
            if (parent == null || parent == curr) break
            curr = parent
        }
        return node
    }

    private fun findLongClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var curr: AccessibilityNodeInfo? = node
        while (curr != null) {
            if (curr.isLongClickable) return curr
            val parent = try { curr.parent } catch (e: Exception) { null }
            if (parent == null || parent == curr) break
            curr = parent
        }
        return node
    }

    private fun findScrollableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var curr: AccessibilityNodeInfo? = node
        while (curr != null) {
            if (curr.isScrollable) return curr
            val parent = try { curr.parent } catch (e: Exception) { null }
            if (parent == null || parent == curr) break
            curr = parent
        }
        return node
    }

    companion object {
        private const val TAG = "ActionExecutor"
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun findNodeByBounds(root: AccessibilityNodeInfo, left: Int, top: Int, right: Int, bottom: Int): AccessibilityNodeInfo? {
        val rect = Rect()
        root.getBoundsInScreen(rect)
        if (rect.left == left && rect.top == top && rect.right == right && rect.bottom == bottom) {
            return root
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeByBounds(child, left, top, right, bottom)
            if (found != null) return found
        }
        return null
    }
}
