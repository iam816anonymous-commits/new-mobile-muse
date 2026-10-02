package com.agent.android.actions

import com.agent.android.observation.ObservationSnapshot
import com.agent.android.target.ResolvedTarget
import org.json.JSONObject

enum class UiActionType {
    CLICK,
    LONG_CLICK,
    TEXT_INPUT,
    SCROLL_FORWARD,
    SCROLL_BACKWARD,
    GLOBAL_BACK,
    GLOBAL_HOME,
    GLOBAL_RECENTS
}

enum class ActionExecutionStatus {
    NOT_STARTED,
    VALIDATING,
    EXECUTING,
    SUCCESS,
    TARGET_NOT_FOUND,
    TARGET_STALE,
    TARGET_NOT_ACTIONABLE,
    WRONG_PACKAGE,
    WRONG_FOREGROUND_APP,
    ACCESSIBILITY_UNAVAILABLE,
    ACTION_UNSUPPORTED,
    ACTION_REJECTED,
    ACTION_FAILED,
    TIMEOUT,
    CANCELLED,
    VERIFICATION_FAILED
}

data class UiActionRequest(
    val requestId: String = "act-${System.currentTimeMillis()}",
    val actionType: UiActionType,
    val targetQueryText: String? = null,
    val targetNodeId: String? = null,
    val resolvedTarget: ResolvedTarget? = null,
    val expectedPackage: String? = null,
    val textInput: String? = null,
    val sourceSnapshotId: String? = null,
    val sourceSnapshotTimestampMs: Long = 0L,
    val timeoutMs: Long = 5000L,
    val verifyPostActionState: Boolean = true
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("requestId", requestId)
        json.put("actionType", actionType.name)
        json.put("targetQueryText", targetQueryText ?: "")
        json.put("targetNodeId", targetNodeId ?: "")
        json.put("expectedPackage", expectedPackage ?: "")
        json.put("textInput", textInput ?: "")
        json.put("sourceSnapshotId", sourceSnapshotId ?: "")
        json.put("sourceSnapshotTimestampMs", sourceSnapshotTimestampMs)
        json.put("timeoutMs", timeoutMs)
        json.put("verifyPostActionState", verifyPostActionState)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): UiActionRequest {
            return UiActionRequest(
                requestId = json.optString("requestId", "act-0"),
                actionType = UiActionType.valueOf(json.optString("actionType", "CLICK")),
                targetQueryText = json.optString("targetQueryText").ifEmpty { null },
                targetNodeId = json.optString("targetNodeId").ifEmpty { null },
                expectedPackage = json.optString("expectedPackage").ifEmpty { null },
                textInput = json.optString("textInput").ifEmpty { null },
                sourceSnapshotId = json.optString("sourceSnapshotId").ifEmpty { null },
                sourceSnapshotTimestampMs = json.optLong("sourceSnapshotTimestampMs", 0L),
                timeoutMs = json.optLong("timeoutMs", 5000L),
                verifyPostActionState = json.optBoolean("verifyPostActionState", true)
            )
        }
    }
}

data class UiActionResult(
    val requestId: String,
    val status: ActionExecutionStatus,
    val actionType: UiActionType,
    val expectedPackage: String? = null,
    val actualPackage: String? = null,
    val actualActivity: String? = null,
    val targetNodeId: String? = null,
    val beforeSnapshot: ObservationSnapshot? = null,
    val afterSnapshot: ObservationSnapshot? = null,
    val explanation: String,
    val durationMs: Long,
    val stateChanged: Boolean = false
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("requestId", requestId)
        json.put("status", status.name)
        json.put("actionType", actionType.name)
        json.put("expectedPackage", expectedPackage ?: "")
        json.put("actualPackage", actualPackage ?: "")
        json.put("actualActivity", actualActivity ?: "")
        json.put("targetNodeId", targetNodeId ?: "")
        json.put("explanation", explanation)
        json.put("durationMs", durationMs)
        json.put("stateChanged", stateChanged)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): UiActionResult {
            return UiActionResult(
                requestId = json.optString("requestId", "act-0"),
                status = ActionExecutionStatus.valueOf(json.optString("status", "ACTION_FAILED")),
                actionType = UiActionType.valueOf(json.optString("actionType", "CLICK")),
                expectedPackage = json.optString("expectedPackage").ifEmpty { null },
                actualPackage = json.optString("actualPackage").ifEmpty { null },
                actualActivity = json.optString("actualActivity").ifEmpty { null },
                targetNodeId = json.optString("targetNodeId").ifEmpty { null },
                explanation = json.optString("explanation", ""),
                durationMs = json.optLong("durationMs", 0L),
                stateChanged = json.optBoolean("stateChanged", false)
            )
        }
    }
}
