package com.agent.android.observation

import org.json.JSONObject

data class ObservedWindow(
    val windowId: Int,
    val packageName: String,
    val activityName: String?,
    val windowType: Int,
    val layer: Int,
    val bounds: ObservationBounds,
    val isActive: Boolean,
    val isFocused: Boolean,
    val isAccessibilityFocused: Boolean,
    val classification: WindowClassification,
    val rootNode: ObservationNode? = null,
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("windowId", windowId)
        obj.put("packageName", packageName)
        obj.put("activityName", activityName ?: JSONObject.NULL)
        obj.put("windowType", windowType)
        obj.put("layer", layer)
        obj.put("bounds", bounds.toJsonObject())
        obj.put("active", isActive)
        obj.put("focused", isFocused)
        obj.put("accessibilityFocused", isAccessibilityFocused)
        obj.put("classification", classification.name)
        obj.put("timestampMs", timestampMs)
        obj.put("rootNode", rootNode?.toJsonObject() ?: JSONObject.NULL)
        return obj
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ObservedWindow {
            val rootObj = json.optJSONObject("rootNode")
            val rootNode = if (rootObj != null) ObservationNode.fromJsonObject(rootObj) else null
            val cls = try {
                WindowClassification.valueOf(json.optString("classification", WindowClassification.UNKNOWN.name))
            } catch (e: Exception) {
                WindowClassification.UNKNOWN
            }

            return ObservedWindow(
                windowId = json.optInt("windowId", -1),
                packageName = json.optString("packageName", "UNKNOWN"),
                activityName = if (json.isNull("activityName")) null else json.optString("activityName"),
                windowType = json.optInt("windowType", 0),
                layer = json.optInt("layer", 0),
                bounds = ObservationBounds.fromJsonObject(json.optJSONObject("bounds")),
                isActive = json.optBoolean("active", false),
                isFocused = json.optBoolean("focused", false),
                isAccessibilityFocused = json.optBoolean("accessibilityFocused", false),
                classification = cls,
                rootNode = rootNode,
                timestampMs = json.optLong("timestampMs", 0L)
            )
        }
    }
}
