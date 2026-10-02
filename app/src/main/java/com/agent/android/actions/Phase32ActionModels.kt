package com.agent.android.actions

import org.json.JSONObject

enum class ActionType {
    CLICK,
    LONG_CLICK,
    TEXT_INPUT,
    SCROLL,
    BACK,
    CUSTOM
}

enum class ActionStatus {
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
    VERIFICATION_FAILED,
    VALIDATION_FAILED
}

enum class ActionExecutionState {
    IDLE,
    VALIDATING,
    READY,
    EXECUTING,
    EXECUTED,
    OBSERVING_RESULT,
    VERIFYING,
    COMPLETED,
    TERMINATED_WITH_ERROR
}

data class ActionRequest(
    val actionId: String,
    val actionType: ActionType,
    val targetIdentity: String? = null,
    val targetResourceId: String? = null,
    val targetText: String? = null,
    val expectedPackage: String? = null,
    val expectedTargetProperties: Map<String, String>? = null,
    val sourceSnapshotId: String? = null,
    val inputText: String? = null,
    val scrollDirection: String? = null,
    val timeoutMs: Long = 5000L,
    val requiresVerification: Boolean = true,
    val executionMetadata: Map<String, String> = emptyMap()
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("actionId", actionId)
        json.put("actionType", actionType.name)
        json.put("targetIdentity", targetIdentity)
        json.put("targetResourceId", targetResourceId)
        json.put("targetText", targetText)
        json.put("expectedPackage", expectedPackage)
        json.put("sourceSnapshotId", sourceSnapshotId)
        json.put("inputText", inputText)
        json.put("scrollDirection", scrollDirection)
        json.put("timeoutMs", timeoutMs)
        json.put("requiresVerification", requiresVerification)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ActionRequest {
            val typeStr = json.optString("actionType", ActionType.CLICK.name)
            val actionType = try { ActionType.valueOf(typeStr) } catch (e: Exception) { ActionType.CLICK }
            return ActionRequest(
                actionId = json.optString("actionId", "action_${System.currentTimeMillis()}"),
                actionType = actionType,
                targetIdentity = json.optString("targetIdentity").takeIf { it.isNotEmpty() },
                targetResourceId = json.optString("targetResourceId").takeIf { it.isNotEmpty() },
                targetText = json.optString("targetText").takeIf { it.isNotEmpty() },
                expectedPackage = json.optString("expectedPackage").takeIf { it.isNotEmpty() },
                sourceSnapshotId = json.optString("sourceSnapshotId").takeIf { it.isNotEmpty() },
                inputText = json.optString("inputText").takeIf { it.isNotEmpty() },
                scrollDirection = json.optString("scrollDirection").takeIf { it.isNotEmpty() },
                timeoutMs = json.optLong("timeoutMs", 5000L),
                requiresVerification = json.optBoolean("requiresVerification", true)
            )
        }
    }
}

data class ActionValidationResult(
    val isValid: Boolean,
    val status: ActionStatus,
    val reason: String,
    val targetFound: Boolean = false,
    val targetVisible: Boolean = false,
    val targetEnabled: Boolean = false,
    val targetSupportsAction: Boolean = false,
    val targetIsStale: Boolean = false,
    val foregroundPackageMatch: Boolean = false,
    val accessibilityServiceAvailable: Boolean = false
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("isValid", isValid)
        json.put("status", status.name)
        json.put("reason", reason)
        json.put("targetFound", targetFound)
        json.put("targetVisible", targetVisible)
        json.put("targetEnabled", targetEnabled)
        json.put("targetSupportsAction", targetSupportsAction)
        json.put("targetIsStale", targetIsStale)
        json.put("foregroundPackageMatch", foregroundPackageMatch)
        json.put("accessibilityServiceAvailable", accessibilityServiceAvailable)
        return json
    }
}

data class Phase32ActionResult(
    val actionId: String,
    val actionType: String,
    val status: ActionStatus,
    val message: String,
    val beforeSnapshotId: String? = null,
    val afterSnapshotId: String? = null,
    val targetPackage: String? = null,
    val foregroundPackage: String? = null,
    val verificationSuccess: Boolean? = null,
    val errorCode: String? = null,
    val durationMs: Long = 0L,
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun isSuccess(): Boolean = status == ActionStatus.SUCCESS

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("actionId", actionId)
        json.put("actionType", actionType)
        json.put("status", status.name)
        json.put("message", message)
        json.put("beforeSnapshotId", beforeSnapshotId)
        json.put("afterSnapshotId", afterSnapshotId)
        json.put("targetPackage", targetPackage)
        json.put("foregroundPackage", foregroundPackage)
        json.put("verificationSuccess", verificationSuccess)
        json.put("errorCode", errorCode)
        json.put("durationMs", durationMs)
        json.put("timestampMs", timestampMs)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): Phase32ActionResult {
            val statusStr = json.optString("status", ActionStatus.ACTION_FAILED.name)
            val status = try { ActionStatus.valueOf(statusStr) } catch (e: Exception) { ActionStatus.ACTION_FAILED }
            return Phase32ActionResult(
                actionId = json.optString("actionId", ""),
                actionType = json.optString("actionType", "UNKNOWN"),
                status = status,
                message = json.optString("message", ""),
                beforeSnapshotId = json.optString("beforeSnapshotId").takeIf { it.isNotEmpty() },
                afterSnapshotId = json.optString("afterSnapshotId").takeIf { it.isNotEmpty() },
                targetPackage = json.optString("targetPackage").takeIf { it.isNotEmpty() },
                foregroundPackage = json.optString("foregroundPackage").takeIf { it.isNotEmpty() },
                verificationSuccess = if (json.has("verificationSuccess")) json.optBoolean("verificationSuccess") else null,
                errorCode = json.optString("errorCode").takeIf { it.isNotEmpty() },
                durationMs = json.optLong("durationMs", 0L),
                timestampMs = json.optLong("timestampMs", System.currentTimeMillis())
            )
        }
    }
}
