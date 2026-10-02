package com.agent.android.target

import com.agent.android.observation.ObservationSnapshot

class TargetResolver {

    fun resolve(
        snapshot: ObservationSnapshot?,
        query: TargetQuery,
        activeSnapshotId: String? = null,
        expectedPackage: String? = null
    ): TargetResolutionResult {
        if (snapshot == null) {
            return TargetResolutionResult(
                query = query,
                status = TargetResolutionStatus.NO_SNAPSHOT,
                explanation = "No ObservationSnapshot available for target resolution."
            )
        }

        if (activeSnapshotId != null && snapshot.snapshotId != activeSnapshotId) {
            return TargetResolutionResult(
                query = query,
                status = TargetResolutionStatus.STALE_SNAPSHOT,
                observedPackage = snapshot.packageName,
                snapshotId = snapshot.snapshotId,
                explanation = "Snapshot identity mismatch: active snapshot '$activeSnapshotId' vs target snapshot '${snapshot.snapshotId}' (STALE_SNAPSHOT)."
            )
        }

        if (expectedPackage != null && snapshot.packageName != expectedPackage) {
            return TargetResolutionResult(
                query = query,
                status = TargetResolutionStatus.NOT_FOUND,
                observedPackage = snapshot.packageName,
                snapshotId = snapshot.snapshotId,
                explanation = "Package mismatch: expected '$expectedPackage' but snapshot package is '${snapshot.packageName}'."
            )
        }

        val hasText = !query.text.isNullOrBlank()
        val hasResId = !query.resourceId.isNullOrBlank()
        val hasDesc = !query.contentDescription.isNullOrBlank()
        val hasClass = !query.className.isNullOrBlank()

        if (!hasText && !hasResId && !hasDesc && !hasClass) {
            return TargetResolutionResult(
                query = query,
                status = TargetResolutionStatus.INVALID_QUERY,
                observedPackage = snapshot.packageName,
                snapshotId = snapshot.snapshotId,
                explanation = "Invalid TargetQuery: all text, resourceId, contentDescription, and className query fields are blank."
            )
        }

        val index = ObservationNormalizer.buildIndex(snapshot)
        val normalizedQueryText = ObservationNormalizer.normalizeText(query.text)
        val normalizedQueryDesc = ObservationNormalizer.normalizeText(query.contentDescription)
        val normalizedQueryResId = ObservationNormalizer.normalizeText(query.resourceId)
        val normalizedQueryClass = ObservationNormalizer.normalizeText(query.className)

        val candidates = mutableListOf<Pair<NormalizedNode, MatchScore>>()

        for (normNode in index.nodes) {
            val node = normNode.node
            var score = 0.0f
            val reasons = mutableListOf<TargetMatchReason>()

            // Package constraint
            if (query.packageName != null && node.packageName != query.packageName) {
                continue
            }

            // Exact Resource ID match
            if (hasResId) {
                if (node.resourceId != null && node.resourceId.equals(query.resourceId, ignoreCase = true)) {
                    score += 10.0f
                    reasons.add(TargetMatchReason.RESOURCE_ID_EXACT)
                } else if (normNode.normalizedResourceId != null && normalizedQueryResId != null && normNode.normalizedResourceId.contains(normalizedQueryResId)) {
                    score += 6.0f
                    reasons.add(TargetMatchReason.RESOURCE_ID_EXACT)
                } else if (hasResId && !hasText && !hasDesc) {
                    continue
                }
            }

            // Exact Text match
            if (hasText) {
                if (node.text != null && node.text.equals(query.text, ignoreCase = false)) {
                    score += 8.0f
                    reasons.add(TargetMatchReason.TEXT_EXACT)
                } else if (normNode.normalizedText != null && normalizedQueryText != null && normNode.normalizedText == normalizedQueryText) {
                    score += 6.0f
                    reasons.add(TargetMatchReason.TEXT_NORMALIZED)
                } else if (normNode.normalizedText != null && normalizedQueryText != null && normNode.normalizedText.contains(normalizedQueryText)) {
                    score += 4.0f
                    reasons.add(TargetMatchReason.TEXT_NORMALIZED)
                } else if (hasText && !hasResId && !hasDesc && score < 1.0f) {
                    continue
                }
            }

            // Content Description match
            if (hasDesc) {
                if (node.contentDescription != null && node.contentDescription.equals(query.contentDescription, ignoreCase = false)) {
                    score += 7.0f
                    reasons.add(TargetMatchReason.CONTENT_DESCRIPTION_EXACT)
                } else if (normNode.normalizedContentDescription != null && normalizedQueryDesc != null && normNode.normalizedContentDescription == normalizedQueryDesc) {
                    score += 5.0f
                    reasons.add(TargetMatchReason.CONTENT_DESCRIPTION_NORMALIZED)
                } else if (normNode.normalizedContentDescription != null && normalizedQueryDesc != null && normNode.normalizedContentDescription.contains(normalizedQueryDesc)) {
                    score += 3.0f
                    reasons.add(TargetMatchReason.CONTENT_DESCRIPTION_NORMALIZED)
                }
            }

            // Class Name match
            if (hasClass) {
                if (node.className != null && node.className.equals(query.className, ignoreCase = true)) {
                    score += 2.0f
                    reasons.add(TargetMatchReason.CLASS_NAME_MATCH)
                } else if (normNode.normalizedClassName != null && normalizedQueryClass != null && normNode.normalizedClassName.contains(normalizedQueryClass)) {
                    score += 1.0f
                    reasons.add(TargetMatchReason.CLASS_NAME_MATCH)
                }
            }

            // Semantic Aliases
            if (hasText) {
                val qLower = query.text?.lowercase() ?: ""
                if (qLower.contains("button") && (node.isClickable || normNode.normalizedClassName?.contains("button") == true)) {
                    score += 3.0f
                    reasons.add(TargetMatchReason.SEMANTIC_MATCH)
                } else if ((qLower.contains("search") || qLower.contains("input") || qLower.contains("box")) && (node.isEditable || normNode.normalizedClassName?.contains("edittext") == true)) {
                    score += 3.0f
                    reasons.add(TargetMatchReason.SEMANTIC_MATCH)
                }
            }

            // State constraints
            if (query.isClickable != null && node.isClickable == query.isClickable) {
                score += 1.0f
                reasons.add(TargetMatchReason.CLICKABLE_MATCH)
            } else if (node.isClickable) {
                score += 0.5f
            }

            if (query.isEditable != null && node.isEditable == query.isEditable) {
                score += 1.0f
                reasons.add(TargetMatchReason.EDITABLE_MATCH)
            }

            if (query.isEnabled != null && node.isEnabled != query.isEnabled) {
                continue
            }

            if (query.isVisible != null && node.isVisibleToUser != query.isVisible) {
                continue
            }

            if (score > 0.0f) {
                candidates.add(normNode to MatchScore(score, reasons))
            }
        }

        if (candidates.isEmpty()) {
            return TargetResolutionResult(
                query = query,
                status = TargetResolutionStatus.NOT_FOUND,
                observedPackage = snapshot.packageName,
                snapshotId = snapshot.snapshotId,
                explanation = "No target candidate matched query constraints in snapshot '${snapshot.snapshotId}' (${snapshot.nodeCount} nodes inspected)."
            )
        }

        val sorted = candidates.sortedByDescending { it.second.score }
        val topScore = sorted.first().second.score

        val topCandidatesList = sorted.take(5).map { (normNode, matchScore) ->
            TargetCandidate(
                nodeId = normNode.node.id,
                text = normNode.node.text,
                contentDescription = normNode.node.contentDescription,
                resourceId = normNode.node.resourceId,
                className = normNode.node.className,
                score = matchScore.score,
                matchReasons = matchScore.reasons
            )
        }

        // Ambiguity Check: if top 2 candidates have identical top score >= 5.0f
        if (sorted.size >= 2) {
            val secondScore = sorted[1].second.score
            if (topScore >= 5.0f && topScore == secondScore && sorted[0].first.node.id != sorted[1].first.node.id) {
                return TargetResolutionResult(
                    query = query,
                    status = TargetResolutionStatus.AMBIGUOUS,
                    candidatesCount = candidates.size,
                    topCandidates = topCandidatesList,
                    observedPackage = snapshot.packageName,
                    snapshotId = snapshot.snapshotId,
                    explanation = "Ambiguous resolution: 2 or more candidates matched with identical top score ($topScore). Disambiguate using resourceId or contentDescription."
                )
            }
        }

        val best = sorted.first()
        val bestNode = best.first.node
        val isActionable = bestNode.isClickable || bestNode.isEditable || bestNode.isCheckable || bestNode.isScrollable

        val resolvedTarget = ResolvedTarget(
            nodeId = bestNode.id,
            node = bestNode,
            packageName = snapshot.packageName,
            activityName = snapshot.activityName,
            snapshotId = snapshot.snapshotId,
            snapshotTimestampMs = snapshot.timestampMs,
            bounds = bestNode.bounds,
            isActionable = isActionable,
            isEditable = bestNode.isEditable,
            isScrollable = bestNode.isScrollable,
            isCheckable = bestNode.isCheckable,
            matchScore = best.second.score,
            matchReasons = best.second.reasons
        )

        return TargetResolutionResult(
            query = query,
            status = TargetResolutionStatus.RESOLVED,
            resolvedTarget = resolvedTarget,
            candidatesCount = candidates.size,
            topCandidates = topCandidatesList,
            observedPackage = snapshot.packageName,
            snapshotId = snapshot.snapshotId,
            explanation = "Target resolved to node '${bestNode.id}' (${bestNode.className?.substringAfterLast('.')}) with score ${best.second.score}."
        )
    }

    private data class MatchScore(
        val score: Float,
        val reasons: List<TargetMatchReason>
    )
}
