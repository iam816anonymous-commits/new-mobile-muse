package com.agent.android.observation

import org.json.JSONArray
import org.json.JSONObject

enum class ObservationMode {
    STOPPED,
    READY,
    OBSERVING
}

enum class ObservationSource {
    LIVE_ACTIVE_WINDOW,
    ACCESSIBLE_WINDOW_QUERY,
    GUIDED_TEST,
    EXPLICIT_CAPTURE,
    RESTORED_SNAPSHOT
}

enum class ObservationScope {
    CURRENT_WINDOW,
    TARGET_APPLICATION,
    SYSTEM_UI,
    LOCAL_AGENT,
    ACCESSIBLE_WINDOWS
}

data class ObservationBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("left", left)
            put("top", top)
            put("right", right)
            put("bottom", bottom)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject?): ObservationBounds {
            if (json == null) return ObservationBounds(0, 0, 0, 0)
            return ObservationBounds(
                left = json.optInt("left", 0),
                top = json.optInt("top", 0),
                right = json.optInt("right", 0),
                bottom = json.optInt("bottom", 0)
            )
        }
    }
}

data class ObservationNode(
    val id: String,
    val parentId: String?,
    val className: String?,
    val packageName: String?,
    val text: String?,
    val contentDescription: String?,
    val resourceId: String?,
    val bounds: ObservationBounds,
    val isClickable: Boolean,
    val isLongClickable: Boolean,
    val isFocusable: Boolean,
    val isFocused: Boolean,
    val isEnabled: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val isCheckable: Boolean,
    val isChecked: Boolean,
    val isSelected: Boolean,
    val isVisibleToUser: Boolean,
    val isPassword: Boolean,
    val childCount: Int,
    val children: List<ObservationNode> = emptyList()
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("parentId", parentId ?: JSONObject.NULL)
        obj.put("className", className ?: JSONObject.NULL)
        obj.put("packageName", packageName ?: JSONObject.NULL)
        obj.put("text", text ?: JSONObject.NULL)
        obj.put("contentDescription", contentDescription ?: JSONObject.NULL)
        obj.put("resourceId", resourceId ?: JSONObject.NULL)
        obj.put("bounds", bounds.toJsonObject())
        obj.put("clickable", isClickable)
        obj.put("longClickable", isLongClickable)
        obj.put("focusable", isFocusable)
        obj.put("focused", isFocused)
        obj.put("enabled", isEnabled)
        obj.put("editable", isEditable)
        obj.put("scrollable", isScrollable)
        obj.put("checkable", isCheckable)
        obj.put("checked", isChecked)
        obj.put("selected", isSelected)
        obj.put("visible", isVisibleToUser)
        obj.put("password", isPassword)
        obj.put("childCount", childCount)

        val childrenArray = JSONArray()
        for (child in children) {
            childrenArray.put(child.toJsonObject())
        }
        obj.put("children", childrenArray)
        return obj
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ObservationNode {
            val childrenList = mutableListOf<ObservationNode>()
            val childrenArray = json.optJSONArray("children")
            if (childrenArray != null) {
                for (i in 0 until childrenArray.length()) {
                    val childObj = childrenArray.optJSONObject(i)
                    if (childObj != null) {
                        childrenList.add(fromJsonObject(childObj))
                    }
                }
            }

            return ObservationNode(
                id = json.optString("id", ""),
                parentId = if (json.isNull("parentId")) null else json.optString("parentId"),
                className = if (json.isNull("className")) null else json.optString("className"),
                packageName = if (json.isNull("packageName")) null else json.optString("packageName"),
                text = if (json.isNull("text")) null else json.optString("text"),
                contentDescription = if (json.isNull("contentDescription")) null else json.optString("contentDescription"),
                resourceId = if (json.isNull("resourceId")) null else json.optString("resourceId"),
                bounds = ObservationBounds.fromJsonObject(json.optJSONObject("bounds")),
                isClickable = json.optBoolean("clickable", false),
                isLongClickable = json.optBoolean("longClickable", false),
                isFocusable = json.optBoolean("focusable", false),
                isFocused = json.optBoolean("focused", false),
                isEnabled = json.optBoolean("enabled", true),
                isEditable = json.optBoolean("editable", false),
                isScrollable = json.optBoolean("scrollable", false),
                isCheckable = json.optBoolean("checkable", false),
                isChecked = json.optBoolean("checked", false),
                isSelected = json.optBoolean("selected", false),
                isVisibleToUser = json.optBoolean("visible", true),
                isPassword = json.optBoolean("password", false),
                childCount = json.optInt("childCount", childrenList.size),
                children = childrenList
            )
        }
    }
}

enum class ObservationState {
    SUCCESS,
    ACCESSIBILITY_DISABLED,
    NO_ACTIVE_WINDOW,
    NO_ACCESSIBLE_WINDOWS,
    ROOT_NODE_UNAVAILABLE,
    WINDOW_NOT_EXPOSED,
    SUPPORTED_SURFACE_BUT_NOT_EXPOSED,
    TARGET_NOT_FOUND,
    OBSERVATION_FAILED,
    CANCELLED
}

data class ObservationSnapshot(
    val timestampMs: Long,
    val snapshotId: String = "snap-${timestampMs}",
    val packageName: String,
    val activityName: String?,
    val windowType: String?,
    val rootBounds: ObservationBounds,
    val nodeCount: Int,
    val rootNode: ObservationNode?,
    val allNodesList: List<ObservationNode>,
    val state: ObservationState,
    val error: String? = null,
    val classification: WindowClassification = WindowClassification.classify(packageName, activityName),
    val source: ObservationSource = ObservationSource.LIVE_ACTIVE_WINDOW,
    val scope: ObservationScope = ObservationScope.CURRENT_WINDOW,
    val testRunId: String? = null
) {
    fun toJsonString(): String {
        val obj = JSONObject()
        obj.put("snapshotId", snapshotId)
        obj.put("timestampMs", timestampMs)
        obj.put("packageName", packageName)
        obj.put("activityName", activityName ?: JSONObject.NULL)
        obj.put("windowType", windowType ?: JSONObject.NULL)
        obj.put("rootBounds", rootBounds.toJsonObject())
        obj.put("nodeCount", nodeCount)
        obj.put("state", state.name)
        obj.put("classification", classification.name)
        obj.put("source", source.name)
        obj.put("scope", scope.name)
        obj.put("testRunId", testRunId ?: JSONObject.NULL)
        obj.put("error", error ?: JSONObject.NULL)
        obj.put("rootNode", rootNode?.toJsonObject() ?: JSONObject.NULL)
        return obj.toString(2)
    }

    companion object {
        fun fromJsonString(jsonStr: String): ObservationSnapshot {
            val json = JSONObject(jsonStr)
            val state = try {
                ObservationState.valueOf(json.optString("state", ObservationState.OBSERVATION_FAILED.name))
            } catch (e: Exception) {
                ObservationState.OBSERVATION_FAILED
            }

            val source = try {
                ObservationSource.valueOf(json.optString("source", ObservationSource.LIVE_ACTIVE_WINDOW.name))
            } catch (e: Exception) {
                ObservationSource.LIVE_ACTIVE_WINDOW
            }

            val scope = try {
                ObservationScope.valueOf(json.optString("scope", ObservationScope.CURRENT_WINDOW.name))
            } catch (e: Exception) {
                ObservationScope.CURRENT_WINDOW
            }

            val rootNodeObj = json.optJSONObject("rootNode")
            val rootNode = if (rootNodeObj != null) ObservationNode.fromJsonObject(rootNodeObj) else null

            val allNodes = mutableListOf<ObservationNode>()
            fun collectNodes(node: ObservationNode?) {
                if (node == null) return
                allNodes.add(node)
                for (child in node.children) {
                    collectNodes(child)
                }
            }
            collectNodes(rootNode)

            val pkg = json.optString("packageName", "UNKNOWN")
            val act = if (json.isNull("activityName")) null else json.optString("activityName")

            val testRunId = if (json.isNull("testRunId")) null else json.optString("testRunId")
            val snapId = json.optString("snapshotId", "snap-${json.optLong("timestampMs", 0L)}")

            return ObservationSnapshot(
                timestampMs = json.optLong("timestampMs", 0L),
                snapshotId = snapId,
                packageName = pkg,
                activityName = act,
                windowType = if (json.isNull("windowType")) null else json.optString("windowType"),
                rootBounds = ObservationBounds.fromJsonObject(json.optJSONObject("rootBounds")),
                nodeCount = json.optInt("nodeCount", allNodes.size),
                rootNode = rootNode,
                allNodesList = allNodes,
                state = state,
                error = if (json.isNull("error")) null else json.optString("error"),
                classification = WindowClassification.classify(pkg, act),
                source = source,
                scope = scope,
                testRunId = testRunId
            )
        }
    }
}

data class ObservationMetadata(
    val isAccessibilityInstalled: Boolean,
    val isAccessibilityEnabled: Boolean,
    val isServiceConnected: Boolean,
    val isRootAvailable: Boolean,
    val lastEventTimestampMs: Long?,
    val lastEventPackageName: String?,
    val lastEventType: String?,
    val lastObservationTimestampMs: Long?,
    val lastObservationNodeCount: Int?,
    val lastError: String?
)
