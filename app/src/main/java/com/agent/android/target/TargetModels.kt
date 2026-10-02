package com.agent.android.target

import com.agent.android.observation.ObservationBounds
import com.agent.android.observation.ObservationNode

enum class TargetSelector {
    AUTO,
    EXACT_TEXT,
    NORMALIZED_TEXT,
    RESOURCE_ID,
    CONTENT_DESCRIPTION,
    CLASS_NAME,
    SEMANTIC_ALIAS,
    COMPOSITE
}

enum class TargetResolutionStatus {
    UNRESOLVED,
    RESOLVED,
    AMBIGUOUS,
    NOT_FOUND,
    INVALID_QUERY,
    NO_SNAPSHOT,
    STALE_SNAPSHOT,
    TARGET_NOT_ACTIONABLE
}

enum class TargetMatchReason {
    RESOURCE_ID_EXACT,
    TEXT_EXACT,
    TEXT_NORMALIZED,
    CONTENT_DESCRIPTION_EXACT,
    CONTENT_DESCRIPTION_NORMALIZED,
    CLASS_NAME_MATCH,
    PACKAGE_NAME_MATCH,
    SEMANTIC_MATCH,
    CLICKABLE_MATCH,
    EDITABLE_MATCH,
    VISIBLE_MATCH
}

data class TargetQuery(
    val text: String? = null,
    val contentDescription: String? = null,
    val resourceId: String? = null,
    val className: String? = null,
    val packageName: String? = null,
    val isClickable: Boolean? = null,
    val isEditable: Boolean? = null,
    val isEnabled: Boolean? = null,
    val isVisible: Boolean? = null,
    val selectorType: TargetSelector = TargetSelector.AUTO
)

data class TargetCandidate(
    val nodeId: String,
    val text: String?,
    val contentDescription: String?,
    val resourceId: String?,
    val className: String?,
    val score: Float,
    val matchReasons: List<TargetMatchReason>
)

data class ResolvedTarget(
    val nodeId: String,
    val node: ObservationNode,
    val packageName: String,
    val activityName: String?,
    val snapshotId: String,
    val snapshotTimestampMs: Long,
    val bounds: ObservationBounds,
    val isActionable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val isCheckable: Boolean,
    val matchScore: Float,
    val matchReasons: List<TargetMatchReason>
)

data class TargetResolutionResult(
    val query: TargetQuery,
    val status: TargetResolutionStatus,
    val resolvedTarget: ResolvedTarget? = null,
    val candidatesCount: Int = 0,
    val topCandidates: List<TargetCandidate> = emptyList(),
    val observedPackage: String? = null,
    val snapshotId: String? = null,
    val explanation: String
)
