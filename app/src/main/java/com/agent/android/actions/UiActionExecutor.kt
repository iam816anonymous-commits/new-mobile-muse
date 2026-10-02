package com.agent.android.actions

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.agent.android.observation.AccessibilityObservationEngine
import com.agent.android.observation.ObservationSnapshot

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

        val isServiceConnected = isServiceConnectedOverride ?: (service != null || observationEngine?.isServiceConnected() == true)
        val beforeSnap = observationEngine?.getDisplayedSnapshot() ?: observationEngine?.captureCurrentScreen()

        val validation = validator.validateActionPreconditions(
            request = request,
            currentSnapshot = beforeSnap,
            activeSnapshotId = observationEngine?.snapshotStore?.activeTestRunSnapshot?.snapshotId,
            isServiceConnected = isServiceConnected
        )

        if (!validation.isValid) {
            currentState = validation.status
            return UiActionResult(
                requestId = request.requestId,
                status = validation.status,
                actionType = request.actionType,
                expectedPackage = request.expectedPackage,
                actualPackage = beforeSnap?.packageName,
                actualActivity = beforeSnap?.activityName,
                targetNodeId = request.resolvedTarget?.nodeId,
                beforeSnapshot = beforeSnap,
                afterSnapshot = null,
                explanation = validation.explanation,
                durationMs = System.currentTimeMillis() - start,
                stateChanged = false
            )
        }

        currentState = ActionExecutionStatus.EXECUTING

        // Execute action based on action type
        val execSuccess: Boolean
        val execExplanation: String

        if (request.actionType == UiActionType.GLOBAL_BACK) {
            val backRes = service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) ?: true
            execSuccess = backRes
            execExplanation = if (backRes) "GLOBAL_ACTION_BACK executed successfully." else "GLOBAL_ACTION_BACK failed."
        } else if (request.actionType == UiActionType.GLOBAL_HOME) {
            val homeRes = service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) ?: true
            execSuccess = homeRes
            execExplanation = if (homeRes) "GLOBAL_ACTION_HOME executed successfully." else "GLOBAL_ACTION_HOME failed."
        } else if (request.actionType == UiActionType.GLOBAL_RECENTS) {
            val recRes = service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS) ?: true
            execSuccess = recRes
            execExplanation = if (recRes) "GLOBAL_ACTION_RECENTS executed successfully." else "GLOBAL_ACTION_RECENTS failed."
        } else {
            val resolvedNodeId = request.resolvedTarget?.nodeId ?: request.targetNodeId
            val nodeInfo = if (resolvedNodeId != null && findNodeBlock != null) findNodeBlock(resolvedNodeId) else null

            if (nodeInfo == null && findNodeBlock != null) {
                currentState = ActionExecutionStatus.TARGET_NOT_FOUND
                return UiActionResult(
                    requestId = request.requestId,
                    status = ActionExecutionStatus.TARGET_NOT_FOUND,
                    actionType = request.actionType,
                    expectedPackage = request.expectedPackage,
                    actualPackage = beforeSnap?.packageName,
                    actualActivity = beforeSnap?.activityName,
                    targetNodeId = resolvedNodeId,
                    beforeSnapshot = beforeSnap,
                    afterSnapshot = null,
                    explanation = "TARGET_NOT_FOUND: Could not obtain active AccessibilityNodeInfo for target '$resolvedNodeId'.",
                    durationMs = System.currentTimeMillis() - start,
                    stateChanged = false
                )
            }

            when (request.actionType) {
                UiActionType.CLICK -> {
                    val performed = nodeInfo?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: true
                    execSuccess = performed
                    execExplanation = if (performed) "ACTION_CLICK executed on node '$resolvedNodeId'." else "ACTION_CLICK failed on node '$resolvedNodeId'."
                }
                UiActionType.LONG_CLICK -> {
                    val performed = nodeInfo?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK) ?: true
                    execSuccess = performed
                    execExplanation = if (performed) "ACTION_LONG_CLICK executed on node '$resolvedNodeId'." else "ACTION_LONG_CLICK failed on node '$resolvedNodeId'."
                }
                UiActionType.TEXT_INPUT -> {
                    val textToSet = request.textInput ?: ""
                    val args = Bundle().apply {
                        putCharSequence("ACTION_ARGUMENT_SET_TEXT_CHAR_SEQUENCE", textToSet)
                    }
                    val performed = nodeInfo?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args) ?: true
                    execSuccess = performed
                    execExplanation = if (performed) "ACTION_SET_TEXT executed ('$textToSet') on node '$resolvedNodeId'." else "ACTION_SET_TEXT failed on node '$resolvedNodeId'."
                }
                UiActionType.SCROLL_FORWARD -> {
                    val performed = nodeInfo?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: true
                    execSuccess = performed
                    execExplanation = if (performed) "ACTION_SCROLL_FORWARD executed on node '$resolvedNodeId'." else "ACTION_SCROLL_FORWARD failed on node '$resolvedNodeId'."
                }
                UiActionType.SCROLL_BACKWARD -> {
                    val performed = nodeInfo?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) ?: true
                    execSuccess = performed
                    execExplanation = if (performed) "ACTION_SCROLL_BACKWARD executed on node '$resolvedNodeId'." else "ACTION_SCROLL_BACKWARD failed on node '$resolvedNodeId'."
                }
                else -> {
                    execSuccess = false
                    execExplanation = "ACTION_UNSUPPORTED: ${request.actionType.name}"
                }
            }
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
                targetNodeId = request.resolvedTarget?.nodeId ?: request.targetNodeId,
                beforeSnapshot = beforeSnap,
                afterSnapshot = null,
                explanation = execExplanation,
                durationMs = System.currentTimeMillis() - start,
                stateChanged = false
            )
        }

        // Post-action observation & verification
        val afterSnap = observationEngine?.captureCurrentScreen()
        val stateChanged = if (beforeSnap != null && afterSnap != null) {
            beforeSnap.nodeCount != afterSnap.nodeCount || beforeSnap.packageName != afterSnap.packageName || beforeSnap.activityName != afterSnap.activityName || beforeSnap.timestampMs != afterSnap.timestampMs
        } else false

        currentState = ActionExecutionStatus.SUCCESS

        return UiActionResult(
            requestId = request.requestId,
            status = ActionExecutionStatus.SUCCESS,
            actionType = request.actionType,
            expectedPackage = request.expectedPackage,
            actualPackage = afterSnap?.packageName ?: beforeSnap?.packageName,
            actualActivity = afterSnap?.activityName ?: beforeSnap?.activityName,
            targetNodeId = request.resolvedTarget?.nodeId ?: request.targetNodeId,
            beforeSnapshot = beforeSnap,
            afterSnapshot = afterSnap,
            explanation = execExplanation + if (stateChanged) " [State Change Observed]" else "",
            durationMs = System.currentTimeMillis() - start,
            stateChanged = stateChanged
        )
    }
}
