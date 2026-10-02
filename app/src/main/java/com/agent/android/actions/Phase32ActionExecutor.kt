package com.agent.android.actions

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.agent.android.observation.ScreenObserver

class Phase32ActionExecutor(
    private val validator: ActionValidator = ActionValidator(),
    private val actionController: ActionController = ActionController()
) {

    @Volatile
    var currentState: ActionExecutionState = ActionExecutionState.IDLE
        private set

    fun executeActionRequest(
        request: ActionRequest,
        service: AccessibilityService?,
        currentForegroundPackage: String?,
        node: AccessibilityNodeInfo?,
        screenObserver: ScreenObserver? = null,
        isCancelled: Boolean = false,
        isTimedOut: Boolean = false
    ): Phase32ActionResult {
        val startMs = System.currentTimeMillis()
        currentState = ActionExecutionState.VALIDATING

        // 1. Target Validation
        val valResult = validator.validateAction(
            request = request,
            service = service,
            currentForegroundPackage = currentForegroundPackage,
            node = node,
            isCancelled = isCancelled,
            isTimedOut = isTimedOut
        )

        if (!valResult.isValid) {
            currentState = ActionExecutionState.TERMINATED_WITH_ERROR
            return Phase32ActionResult(
                actionId = request.actionId,
                actionType = request.actionType.name,
                status = valResult.status,
                message = valResult.reason,
                foregroundPackage = currentForegroundPackage,
                durationMs = System.currentTimeMillis() - startMs
            )
        }

        currentState = ActionExecutionState.READY

        // 2. Capture BEFORE snapshot summary
        val beforeSnapshot = screenObserver?.captureLightweightSnapshot()
        val beforeSnapshotId = beforeSnapshot?.let { "snap_${it.packageName}_${it.timestampMs}" }

        // 3. Execution under ActionController Lock
        var executionSuccess = false
        var execMessage = ""
        var execStatus = ActionStatus.SUCCESS

        val legacyAction = object : Action {
            override val id: String = request.actionId
            override val actionType: String = request.actionType.name
            override val description: String = "Phase 3.2 ${request.actionType} action on ${request.targetIdentity ?: "target"}"
        }

        currentState = ActionExecutionState.EXECUTING

        actionController.executeAction(legacyAction) {
            try {
                when (request.actionType) {
                    ActionType.CLICK -> {
                        val performed = performClickInternal(node)
                        if (performed) {
                            executionSuccess = true
                            execMessage = "CLICK action executed successfully"
                        } else {
                            executionSuccess = false
                            execStatus = ActionStatus.ACTION_FAILED
                            execMessage = "Failed to perform CLICK on target node"
                        }
                    }
                    ActionType.LONG_CLICK -> {
                        val performed = performLongClickInternal(node)
                        if (performed) {
                            executionSuccess = true
                            execMessage = "LONG_CLICK action executed successfully"
                        } else {
                            executionSuccess = false
                            execStatus = ActionStatus.ACTION_FAILED
                            execMessage = "Failed to perform LONG_CLICK on target node"
                        }
                    }
                    ActionType.TEXT_INPUT -> {
                        val inputStr = request.inputText ?: ""
                        val performed = performTextInputInternal(node, inputStr)
                        if (performed) {
                            executionSuccess = true
                            execMessage = "TEXT_INPUT '$inputStr' executed successfully"
                        } else {
                            executionSuccess = false
                            execStatus = ActionStatus.ACTION_FAILED
                            execMessage = "Failed to perform TEXT_INPUT on target node"
                        }
                    }
                    ActionType.SCROLL -> {
                        val dir = request.scrollDirection?.uppercase() ?: "FORWARD"
                        val performed = performScrollInternal(node, dir)
                        if (performed) {
                            executionSuccess = true
                            execMessage = "SCROLL $dir executed successfully"
                        } else {
                            executionSuccess = false
                            execStatus = ActionStatus.ACTION_FAILED
                            execMessage = "Failed to perform SCROLL $dir on target node"
                        }
                    }
                    ActionType.BACK -> {
                        val performed = service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) ?: false
                        if (performed) {
                            executionSuccess = true
                            execMessage = "BACK action executed successfully"
                        } else {
                            executionSuccess = false
                            execStatus = ActionStatus.ACTION_FAILED
                            execMessage = "Failed to perform GLOBAL_ACTION_BACK via AccessibilityService"
                        }
                    }
                    ActionType.CUSTOM -> {
                        executionSuccess = true
                        execMessage = "CUSTOM action executed"
                    }
                }
            } catch (e: Exception) {
                executionSuccess = false
                execStatus = ActionStatus.ACTION_FAILED
                execMessage = "Action execution threw exception: ${e.message}"
            }
            ActionResult(actionId = request.actionId, success = executionSuccess, message = execMessage)
        }

        if (!executionSuccess) {
            currentState = ActionExecutionState.TERMINATED_WITH_ERROR
            return Phase32ActionResult(
                actionId = request.actionId,
                actionType = request.actionType.name,
                status = execStatus,
                message = execMessage,
                beforeSnapshotId = beforeSnapshotId,
                foregroundPackage = currentForegroundPackage,
                durationMs = System.currentTimeMillis() - startMs
            )
        }

        currentState = ActionExecutionState.EXECUTED

        // 4. Post-action Observation & Verification
        currentState = ActionExecutionState.OBSERVING_RESULT
        try { Thread.sleep(300L) } catch (_: Exception) {}

        val afterSnapshot = screenObserver?.captureLightweightSnapshot()
        val afterSnapshotId = afterSnapshot?.let { "snap_${it.packageName}_${it.timestampMs}" }

        currentState = ActionExecutionState.VERIFYING
        var verified = true
        var verificationReason = "Post-action observation captured"

        if (request.requiresVerification && screenObserver != null) {
            if (beforeSnapshot != null && afterSnapshot != null) {
                if (request.actionType == ActionType.TEXT_INPUT && !request.inputText.isNullOrEmpty()) {
                    val elements = screenObserver.getInteractiveElementsSummary()
                    val textPresent = elements.any { it.text?.contains(request.inputText) == true }
                    if (!textPresent) {
                        verified = false
                        verificationReason = "Verification failed: Input text '${request.inputText}' not found in post-action observation"
                    }
                } else if (beforeSnapshot.elementCount == afterSnapshot.elementCount && beforeSnapshot.packageName == afterSnapshot.packageName) {
                    verificationReason = "Post-action observation verified (UI unchanged or stable)"
                }
            }
        }

        currentState = ActionExecutionState.COMPLETED

        return Phase32ActionResult(
            actionId = request.actionId,
            actionType = request.actionType.name,
            status = if (verified) ActionStatus.SUCCESS else ActionStatus.VERIFICATION_FAILED,
            message = if (verified) execMessage else verificationReason,
            beforeSnapshotId = beforeSnapshotId,
            afterSnapshotId = afterSnapshotId,
            targetPackage = request.expectedPackage,
            foregroundPackage = currentForegroundPackage,
            verificationSuccess = verified,
            durationMs = System.currentTimeMillis() - startMs
        )
    }

    private fun performClickInternal(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isClickable) {
            val res = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (res) return true
        }
        var parent = node.parent
        var depth = 0
        while (parent != null && depth < 3) {
            if (parent.isClickable) {
                val res = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (res) return true
            }
            parent = parent.parent
            depth++
        }
        return false
    }

    private fun performLongClickInternal(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isLongClickable) {
            val res = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            if (res) return true
        }
        var parent = node.parent
        var depth = 0
        while (parent != null && depth < 3) {
            if (parent.isLongClickable) {
                val res = parent.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                if (res) return true
            }
            parent = parent.parent
            depth++
        }
        return false
    }

    private fun performTextInputInternal(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val res = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (res) return true
        }
        return false
    }

    private fun performScrollInternal(node: AccessibilityNodeInfo?, direction: String): Boolean {
        if (node == null) return false
        val action = if (direction == "BACKWARD" || direction == "UP" || direction == "LEFT") {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }
        if (node.isScrollable) {
            val res = node.performAction(action)
            if (res) return true
        }
        var parent = node.parent
        var depth = 0
        while (parent != null && depth < 3) {
            if (parent.isScrollable) {
                val res = parent.performAction(action)
                if (res) return true
            }
            parent = parent.parent
            depth++
        }
        return false
    }
}
