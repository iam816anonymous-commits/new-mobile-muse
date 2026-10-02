package com.agent.android.actions

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

class ActionValidator {

    fun validateAction(
        request: ActionRequest,
        service: AccessibilityService?,
        currentForegroundPackage: String?,
        node: AccessibilityNodeInfo?,
        isCancelled: Boolean = false,
        isTimedOut: Boolean = false
    ): ActionValidationResult {
        // 1. Cancellation / Timeout check
        if (isCancelled) {
            return ActionValidationResult(
                isValid = false,
                status = ActionStatus.CANCELLED,
                reason = "Action execution was cancelled by user or safety controller"
            )
        }
        if (isTimedOut) {
            return ActionValidationResult(
                isValid = false,
                status = ActionStatus.TIMEOUT,
                reason = "Action execution timed out before validation"
            )
        }

        // 2. Accessibility Service Check
        if (service == null) {
            return ActionValidationResult(
                isValid = false,
                status = ActionStatus.ACCESSIBILITY_UNAVAILABLE,
                reason = "LocalAgentAccessibilityService is not connected or enabled"
            )
        }

        // 3. Foreground Package Validation (Mandatory Wrong Application Protection)
        val fgPkg = currentForegroundPackage ?: ""
        if (request.expectedPackage != null && request.expectedPackage.isNotBlank()) {
            if (!fgPkg.equals(request.expectedPackage, ignoreCase = true)) {
                return ActionValidationResult(
                    isValid = false,
                    status = ActionStatus.WRONG_FOREGROUND_APP,
                    reason = "Current foreground app ($fgPkg) does not match expected package (${request.expectedPackage})",
                    foregroundPackageMatch = false,
                    accessibilityServiceAvailable = true
                )
            }
        }

        // External app action requested, but LocalAgent itself is foreground
        if (request.actionType != ActionType.BACK && fgPkg == "com.agent.android" && request.expectedPackage != null && request.expectedPackage != "com.agent.android") {
            return ActionValidationResult(
                isValid = false,
                status = ActionStatus.WRONG_FOREGROUND_APP,
                reason = "LocalAgent is currently in foreground; external action cannot execute against LocalAgent UI",
                foregroundPackageMatch = false,
                accessibilityServiceAvailable = true
            )
        }

        // 4. BACK action does not require a UI target node
        if (request.actionType == ActionType.BACK) {
            return ActionValidationResult(
                isValid = true,
                status = ActionStatus.SUCCESS,
                reason = "BACK action target validated",
                targetFound = true,
                targetVisible = true,
                targetEnabled = true,
                targetSupportsAction = true,
                foregroundPackageMatch = true,
                accessibilityServiceAvailable = true
            )
        }

        // 5. Target Existence Check
        if (node == null) {
            return ActionValidationResult(
                isValid = false,
                status = ActionStatus.TARGET_NOT_FOUND,
                reason = "Target UI node '${request.targetIdentity ?: request.targetResourceId ?: request.targetText ?: "unknown"}' not found in view tree",
                targetFound = false,
                foregroundPackageMatch = true,
                accessibilityServiceAvailable = true
            )
        }

        // 6. Target Staleness Check
        var isStale = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                val refreshed = node.refresh()
                if (!refreshed && node.packageName == null) {
                    isStale = true
                }
            }
        } catch (e: Exception) {
            isStale = true
        }

        if (isStale) {
            return ActionValidationResult(
                isValid = false,
                status = ActionStatus.TARGET_STALE,
                reason = "Target UI node reference is stale or detached from active window",
                targetFound = true,
                targetIsStale = true,
                foregroundPackageMatch = true,
                accessibilityServiceAvailable = true
            )
        }

        // 7. Node Package Check
        val nodePkg = node.packageName?.toString() ?: ""
        if (request.expectedPackage != null && request.expectedPackage.isNotBlank() && nodePkg.isNotEmpty()) {
            if (!nodePkg.equals(request.expectedPackage, ignoreCase = true)) {
                return ActionValidationResult(
                    isValid = false,
                    status = ActionStatus.WRONG_PACKAGE,
                    reason = "Target node package ($nodePkg) does not match expected package (${request.expectedPackage})",
                    targetFound = true,
                    foregroundPackageMatch = false,
                    accessibilityServiceAvailable = true
                )
            }
        }

        // 8. Target Visible & Enabled Checks
        val isVisible = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) node.isVisibleToUser else true
        val isEnabled = node.isEnabled

        if (!isEnabled) {
            return ActionValidationResult(
                isValid = false,
                status = ActionStatus.TARGET_NOT_ACTIONABLE,
                reason = "Target UI node is disabled",
                targetFound = true,
                targetVisible = isVisible,
                targetEnabled = false,
                foregroundPackageMatch = true,
                accessibilityServiceAvailable = true
            )
        }

        if (!isVisible) {
            return ActionValidationResult(
                isValid = false,
                status = ActionStatus.TARGET_NOT_ACTIONABLE,
                reason = "Target UI node is not visible to user",
                targetFound = true,
                targetVisible = false,
                targetEnabled = true,
                foregroundPackageMatch = true,
                accessibilityServiceAvailable = true
            )
        }

        // 9. Action Capability Support Check
        val supportsAction = when (request.actionType) {
            ActionType.CLICK -> node.isClickable || hasAncestorOrChildAction(node, AccessibilityNodeInfo.ACTION_CLICK)
            ActionType.LONG_CLICK -> node.isLongClickable || hasAncestorOrChildAction(node, AccessibilityNodeInfo.ACTION_LONG_CLICK)
            ActionType.TEXT_INPUT -> (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && node.isEditable) || hasAncestorOrChildAction(node, AccessibilityNodeInfo.ACTION_SET_TEXT)
            ActionType.SCROLL -> node.isScrollable || hasAncestorOrChildAction(node, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) || hasAncestorOrChildAction(node, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            ActionType.BACK -> true
            ActionType.CUSTOM -> true
        }

        if (!supportsAction) {
            return ActionValidationResult(
                isValid = false,
                status = ActionStatus.ACTION_UNSUPPORTED,
                reason = "Target UI node does not support action type '${request.actionType}'",
                targetFound = true,
                targetVisible = true,
                targetEnabled = true,
                targetSupportsAction = false,
                foregroundPackageMatch = true,
                accessibilityServiceAvailable = true
            )
        }

        return ActionValidationResult(
            isValid = true,
            status = ActionStatus.SUCCESS,
            reason = "Target validated successfully for ${request.actionType}",
            targetFound = true,
            targetVisible = true,
            targetEnabled = true,
            targetSupportsAction = true,
            foregroundPackageMatch = true,
            accessibilityServiceAvailable = true
        )
    }

    private fun hasAncestorOrChildAction(node: AccessibilityNodeInfo, actionId: Int): Boolean {
        for (a in node.actionList) {
            if (a.id == actionId) return true
        }
        var p = node.parent
        var depth = 0
        while (p != null && depth < 3) {
            if (p.isClickable && actionId == AccessibilityNodeInfo.ACTION_CLICK) return true
            if (p.isLongClickable && actionId == AccessibilityNodeInfo.ACTION_LONG_CLICK) return true
            if (p.isScrollable && (actionId == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD || actionId == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) return true
            for (a in p.actionList) {
                if (a.id == actionId) return true
            }
            p = p.parent
            depth++
        }
        return false
    }
}
