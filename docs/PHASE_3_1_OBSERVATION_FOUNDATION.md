# Phase 3.1 — Observation Foundation & Universal Architecture

## 1. Executive Summary & Core Principle
Phase 3.1 establishes the perception/observation foundation for LocalAgent on Android 8.1 / API 27 targets. The core architectural directive is:

> **Observation is Universal. Target Selection is Contextual. Interaction is Separate.**

The Observation Engine (`AccessibilityObservationEngine`) does not maintain application allowlists or hardcoded window drop rules at the perception layer. It queries whatever accessibility UI hierarchies Android API 27 exposes, classifies the observed window surfaces, and produces structured, immutable `ObservationSnapshot` models.

## 2. Universal Observation Pipeline
```
Android Accessibility Framework
             ↓
LocalAgentAccessibilityService (onAccessibilityEvent)
             ↓
AccessibleWindowProvider (service.windows / rootInActiveWindow)
             ↓
AccessibilityObservationEngine (Debounced 1000ms, max 500 nodes, depth 30)
             ↓
WindowClassification (APPLICATION, LOCAL_AGENT, SYSTEM_UI, LAUNCHER, RECENTS, SETTINGS, etc.)
             ↓
ObservationSnapshotStore (Thread-safe Canonical Source of Truth with Session Invalidation)
             ↓
Target Policy Resolution (Card A / Card B / Future Agent Planner)
```

## 3. Android API 27 Accessibility Service Configuration
The service configuration in `accessibility_service_config.xml` enables the required capabilities for API 27 interactive window discovery:

```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRequestFilterKeyEvents|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:canRequestFilterKeyEvents="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100" />
```

### Key Configuration Flags:
- `canRetrieveWindowContent="true"`: Allows inspecting `AccessibilityNodeInfo` node trees.
- `flagRetrieveInteractiveWindows`: Enables `AccessibilityService.windows` (available API 21+) to inspect interactive application, system dialog, and overlay windows.
- `notificationTimeout="100"`: Debounces high-frequency event streams at the system level.

## 4. Multi-Window Discovery (`AccessibleWindowProvider`)
`AccessibleWindowProvider` provides a clean abstraction for inspecting interactive windows on API 27:
1. Calls `service.windows` to retrieve the active list of `AccessibilityWindowInfo` objects.
2. For each window, extracts window ID, package name, class name, window type (`TYPE_APPLICATION`, `TYPE_SYSTEM`, `TYPE_INPUT_METHOD`, `TYPE_ACCESSIBILITY_OVERLAY`), layer, active state, and focus state.
3. Classifies each window using `WindowClassification.classify()`.
4. **Fallback Mechanism**: If `service.windows` is null or returns an empty list, falls back to `service.rootInActiveWindow` and wraps the active root into a fallback `ObservedWindow`.

## 5. Snapshot Preservation & Lifecycle Protection
To prevent returning to LocalAgent or pressing `STOP OBSERVATION` from overwriting valid external application snapshots:
- **`ObservationSnapshotStore`**: Manages `currentLiveSnapshot`, `lastValidExternalSnapshot`, `displayedSnapshot`, and atomic `sessionId`.
- **Session Token Invalidation**: `startObservationMode()` and `stopObservationMode()` increment `sessionId`. Any delayed or queued background accessibility event callbacks with an obsolete session token are discarded immediately.
- **STOP Does Not Capture**: Tapping `STOP OBSERVATION` stops future capture without calling `rootInActiveWindow` or overwriting `lastValidExternalSnapshot`.
- **Package Exclusion Filter**: `isExcludedExternalPackage()` excludes LocalAgent (`com.agent.android`), System UI (`com.android.systemui`), launchers, and recents from replacing `lastValidExternalSnapshot`.
