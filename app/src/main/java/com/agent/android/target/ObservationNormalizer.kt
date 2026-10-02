package com.agent.android.target

import com.agent.android.observation.ObservationNode
import com.agent.android.observation.ObservationSnapshot

data class NormalizedNode(
    val node: ObservationNode,
    val normalizedText: String?,
    val normalizedContentDescription: String?,
    val normalizedResourceId: String?,
    val normalizedClassName: String?
)

class TargetIndex(
    val snapshot: ObservationSnapshot,
    val nodes: List<NormalizedNode>
) {
    val byNodeId: Map<String, NormalizedNode> = nodes.associateBy { it.node.id }
}

object ObservationNormalizer {

    fun normalizeText(raw: String?): String? {
        if (raw == null) return null
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.replace("\\s+".toRegex(), " ")
            .lowercase()
            .replace("[.,!?:;\"']".toRegex(), "")
    }

    fun buildIndex(snapshot: ObservationSnapshot): TargetIndex {
        val normalizedList = snapshot.allNodesList.map { node ->
            NormalizedNode(
                node = node,
                normalizedText = normalizeText(node.text),
                normalizedContentDescription = normalizeText(node.contentDescription),
                normalizedResourceId = normalizeText(node.resourceId),
                normalizedClassName = normalizeText(node.className)
            )
        }
        return TargetIndex(snapshot, normalizedList)
    }
}
