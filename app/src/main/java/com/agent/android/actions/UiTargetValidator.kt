package com.agent.android.actions

import com.agent.android.observation.ObservationSnapshot
import com.agent.android.observation.ObservationState
import com.agent.android.target.ResolvedTarget

data class ValidationResult(
    val isValid: Boolean,
    val status: ActionExecutionStatus,
    val explanation: String
)

class UiTargetValidator {

    fun validateActionPreconditions(
        request: UiActionRequest,
        currentSnapshot: ObservationSnapshot?,
        activeSnapshotId: String? = null,
        isServiceConnected: Boolean = true
    ): ValidationResult {
        if (!isServiceConnected) {
            return ValidationResult(
                isValid = false,
                status = ActionExecutionStatus.ACCESSIBILITY_UNAVAILABLE,
                explanation = "ACCESSIBILITY_UNAVAILABLE: Accessibility service is disabled or disconnected."
            )
        }

        if (currentSnapshot == null || currentSnapshot.state != ObservationState.SUCCESS) {
            return ValidationResult(
                isValid = false,
                status = ActionExecutionStatus.TARGET_NOT_FOUND,
                explanation = "TARGET_NOT_FOUND: No active ObservationSnapshot available for target validation."
            )
        }

        val actualPackage = currentSnapshot.packageName
        val expectedPackage = request.expectedPackage

        // Check expected package vs actual foreground package
        if (expectedPackage != null && expectedPackage.isNotBlank()) {
            if (actualPackage != expectedPackage) {
                if (actualPackage == "com.agent.android") {
                    return ValidationResult(
                        isValid = false,
                        status = ActionExecutionStatus.WRONG_FOREGROUND_APP,
                        explanation = "WRONG_FOREGROUND_APP: LocalAgent is foreground; expected target package '$expectedPackage'."
                    )
                }
                return ValidationResult(
                    isValid = false,
                    status = ActionExecutionStatus.WRONG_PACKAGE,
                    explanation = "WRONG_PACKAGE: Current foreground package '$actualPackage' does not match expected package '$expectedPackage'."
                )
            }
        }

        // Global actions do not require target node resolution
        if (request.actionType == UiActionType.GLOBAL_BACK || request.actionType == UiActionType.GLOBAL_HOME || request.actionType == UiActionType.GLOBAL_RECENTS) {
            return ValidationResult(
                isValid = true,
                status = ActionExecutionStatus.SUCCESS,
                explanation = "Global action '${request.actionType.name}' validated."
            )
        }

        // Stale target protection
        if (request.sourceSnapshotId != null && request.sourceSnapshotId.isNotBlank()) {
            val snapshotIdToCompare = activeSnapshotId ?: currentSnapshot.snapshotId
            if (request.sourceSnapshotId != snapshotIdToCompare) {
                return ValidationResult(
                    isValid = false,
                    status = ActionExecutionStatus.TARGET_STALE,
                    explanation = "TARGET_STALE: Source snapshot '${request.sourceSnapshotId}' does not match active snapshot '$snapshotIdToCompare'."
                )
            }
        }

        val resolved = request.resolvedTarget
        if (resolved == null) {
            return ValidationResult(
                isValid = false,
                status = ActionExecutionStatus.TARGET_NOT_FOUND,
                explanation = "TARGET_NOT_FOUND: Action request has no resolved target node."
            )
        }

        val node = resolved.node

        // Target bounds validation
        val width = resolved.bounds.right - resolved.bounds.left
        val height = resolved.bounds.bottom - resolved.bounds.top
        if (width <= 0 || height <= 0) {
            return ValidationResult(
                isValid = false,
                status = ActionExecutionStatus.TARGET_NOT_ACTIONABLE,
                explanation = "TARGET_NOT_ACTIONABLE: Target node has invalid bounds (${resolved.bounds.left},${resolved.bounds.top} -> ${resolved.bounds.right},${resolved.bounds.bottom})."
            )
        }

        // Enabled state check
        if (!node.isEnabled) {
            return ValidationResult(
                isValid = false,
                status = ActionExecutionStatus.TARGET_NOT_ACTIONABLE,
                explanation = "TARGET_NOT_ACTIONABLE: Target node '${node.id}' is disabled."
            )
        }

        // Action-specific capabilities check
        return when (request.actionType) {
            UiActionType.CLICK -> {
                if (!node.isClickable && !resolved.isActionable) {
                    ValidationResult(
                        isValid = false,
                        status = ActionExecutionStatus.ACTION_UNSUPPORTED,
                        explanation = "ACTION_UNSUPPORTED: Target node '${node.id}' is not clickable."
                    )
                } else {
                    ValidationResult(isValid = true, status = ActionExecutionStatus.SUCCESS, explanation = "CLICK action validated for target '${node.id}'.")
                }
            }
            UiActionType.LONG_CLICK -> {
                if (!node.isLongClickable) {
                    ValidationResult(
                        isValid = false,
                        status = ActionExecutionStatus.ACTION_UNSUPPORTED,
                        explanation = "ACTION_UNSUPPORTED: Target node '${node.id}' does not support long click."
                    )
                } else {
                    ValidationResult(isValid = true, status = ActionExecutionStatus.SUCCESS, explanation = "LONG_CLICK action validated for target '${node.id}'.")
                }
            }
            UiActionType.TEXT_INPUT -> {
                if (!node.isEditable && !resolved.isEditable) {
                    ValidationResult(
                        isValid = false,
                        status = ActionExecutionStatus.ACTION_UNSUPPORTED,
                        explanation = "ACTION_UNSUPPORTED: Target node '${node.id}' is not editable."
                    )
                } else {
                    ValidationResult(isValid = true, status = ActionExecutionStatus.SUCCESS, explanation = "TEXT_INPUT action validated for target '${node.id}'.")
                }
            }
            UiActionType.SCROLL_FORWARD, UiActionType.SCROLL_BACKWARD -> {
                if (!node.isScrollable && !resolved.isScrollable) {
                    ValidationResult(
                        isValid = false,
                        status = ActionExecutionStatus.ACTION_UNSUPPORTED,
                        explanation = "ACTION_UNSUPPORTED: Target node '${node.id}' is not scrollable."
                    )
                } else {
                    ValidationResult(isValid = true, status = ActionExecutionStatus.SUCCESS, explanation = "SCROLL action validated for target '${node.id}'.")
                }
            }
            else -> {
                ValidationResult(isValid = true, status = ActionExecutionStatus.SUCCESS, explanation = "Action validated.")
            }
        }
    }
}
