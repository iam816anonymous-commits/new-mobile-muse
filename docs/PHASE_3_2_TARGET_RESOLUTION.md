# Phase 3.2 Target Resolution Architecture

## 1. Overview & Architectural Boundary
Phase 3.2 transforms captured `ObservationSnapshot` UI hierarchies into `ResolvedTarget` instances.

**STRICT READ-ONLY BOUNDARY:** Phase 3.2 does NOT perform any UI clicks, long-clicks, typing, scrolling, gestures, or actions. It answers "Which observed UI element is the user referring to?" without performing execution.

## 2. Pipeline Architecture
```
ObservationSnapshot
        ↓
ObservationNormalizer (Builds TargetIndex with normalized text & resource IDs)
        ↓
TargetQuery (Type-safe selector with text, resourceId, contentDescription, className, package)
        ↓
TargetResolver (Ranking hierarchy & candidate scoring)
        ↓
CandidateRanking (Top candidates with match scores and match reasons)
        ↓
TargetResolutionStatus & ResolvedTarget (RESOLVED, AMBIGUOUS, STALE_SNAPSHOT, NOT_FOUND)
```

## 3. Deterministic Ranking Hierarchy
1. Exact resource ID match (+10.0)
2. Exact text match (+8.0)
3. Exact content description match (+7.0)
4. Normalized text match (+5.0)
5. Normalized content description match (+4.0)
6. Class name match (+2.0)
7. Semantic alias match (e.g. "button" -> clickable node, "search box" -> EditText) (+3.0)
8. Clickable (+1.0) / Enabled (+0.5) / Visible (+0.5)

## 4. Ambiguity & Stale Snapshot Protection
- **Ambiguity Detection**: If two or more candidate nodes achieve an identical top match score (>= 5.0), `TargetResolver` returns `AMBIGUOUS` with top candidates listed rather than randomly selecting candidate 1.
- **Stale Snapshot Protection**: If `snapshot.snapshotId` does not match the active observation snapshot ID, `TargetResolver` returns `STALE_SNAPSHOT`.
- **Package Isolation**: If requested package differs from observed package, returns `NOT_FOUND` / `PACKAGE_MISMATCH`.

## 5. Target Models
- `TargetQuery`: Selector query with text, contentDescription, resourceId, className, packageName.
- `ResolvedTarget`: Identified target node with bounds, actionability properties (`isActionable`, `isEditable`, `isScrollable`, `isCheckable`), score, and match reasons.
- `TargetCandidate`: Ranked candidate node for diagnostics inspection.
- `TargetResolutionResult`: Full resolution output including status, resolved target, and candidate analysis explanation.
