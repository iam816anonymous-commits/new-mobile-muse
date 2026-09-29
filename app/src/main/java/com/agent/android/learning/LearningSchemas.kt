package com.agent.android.learning

import org.json.JSONObject

data class LearningRecord(
    val schemaVersion: Int = 1,
    val recordId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String,
    val confidence: Double,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val lastVerifiedMs: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("schemaVersion", schemaVersion)
        json.put("recordId", recordId)
        json.put("timestamp", timestamp)
        json.put("source", source)
        json.put("confidence", confidence)
        json.put("successCount", successCount)
        json.put("failureCount", failureCount)
        json.put("lastVerifiedMs", lastVerifiedMs)
        return json.toString()
    }

    companion object {
        fun jsonToRecord(jsonString: String): LearningRecord {
            val json = JSONObject(jsonString)
            val version = json.optInt("schemaVersion", 1)
            if (version > 1) {
                throw IllegalArgumentException("Unsupported schema version: $version")
            }
            return LearningRecord(
                schemaVersion = version,
                recordId = json.optString("recordId", ""),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                source = json.optString("source", ""),
                confidence = json.optDouble("confidence", 0.0),
                successCount = json.optInt("successCount", 0),
                failureCount = json.optInt("failureCount", 0),
                lastVerifiedMs = json.optLong("lastVerifiedMs", System.currentTimeMillis())
            )
        }
    }
}

data class WorkflowRecord(
    val schemaVersion: Int = 1,
    val workflowId: String,
    val packageName: String,
    val goalPattern: String,
    val stepCount: Int
)

data class ActionOutcome(
    val schemaVersion: Int = 1,
    val actionType: String,
    val targetIdentifier: String,
    val success: Boolean,
    val executionTimeMs: Long
)

data class AppProfile(
    val schemaVersion: Int = 1,
    val packageName: String,
    val appLabel: String,
    val knownScreens: Int
)

data class DeviceProfile(
    val schemaVersion: Int = 1,
    val deviceModel: String,
    val apiVersion: Int,
    val ramMb: Long
)
