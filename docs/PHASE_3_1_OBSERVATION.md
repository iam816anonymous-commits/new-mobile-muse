# Phase 3.1 — Observation Foundation Architecture

## 1. Overview & Objective
Phase 3.1 establishes the perception/observation foundation for LocalAgent on Android 8.1 / API 27 targets. The observation subsystem inspects currently visible user interface hierarchies via native Android `AccessibilityService` APIs and produces structured, immutable, machine-readable representations (`ObservationSnapshot` / `ObservationNode`).

## 2. Core Architectural Rules & Safety
1. **STRICTLY READ-ONLY**: The Observation Engine (`AccessibilityObservationEngine`) performs zero UI actions, gestures, taps, clicks, text input, or app launches.
2. **OBSERVATION-ONLY SERVICE**: `LocalAgentAccessibilityService.onAccessibilityEvent()` logs lightweight event metadata and maintains `instance` references for observation queries without executing goals or launching autonomous loops.
3. **ZERO RECURSIVE LEAKS**: All `AccessibilityNodeInfo` instances are recycled (`nodeInfo.recycle()`) during traversal to protect target Tecno Camon i (4GB RAM) hardware.
4. **TRAVERSAL BOUNDS**: Maximum node count is strictly capped at `500` nodes and depth is capped at `30` levels to prevent thread starvation or stack overflows.

## 3. Architecture Component Diagram
```
Android Accessibility Subsystem
             ↓
LocalAgentAccessibilityService (rootInActiveWindow)
             ↓
AccessibilityObservationEngine (Traverses & Recycles)
             ↓
ObservationSnapshot / ObservationNode (Immutable Data Models)
             ↓
JSON Serialization / Phase 3.1 UI Inspector / Target Resolver
```

## 4. Data Models & JSON Schema
### ObservationNode Properties
- `id`: Unique generated node identifier (e.g. `node-1`)
- `parentId`: Parent node identifier
- `className`: Android widget class (e.g. `android.widget.Button`)
- `packageName`: App package name
- `text`: Visible view text
- `contentDescription`: Accessibility content description
- `resourceId`: View resource ID (`viewIdResourceName`)
- `bounds`: Screen bounds rectangle (`left`, `top`, `right`, `bottom`)
- `isClickable`, `isLongClickable`, `isFocusable`, `isFocused`, `isEnabled`, `isEditable`, `isScrollable`, `isCheckable`, `isChecked`, `isSelected`, `isVisibleToUser`, `isPassword`
- `childCount`, `children`: Preserved parent-child hierarchy list

### ObservationSnapshot Properties
- `timestampMs`: Epoch millisecond timestamp of capture
- `packageName`: Foreground app package name
- `activityName`: Active activity or window class name
- `rootBounds`: Root screen dimensions
- `nodeCount`: Total number of parsed nodes
- `rootNode`: Root `ObservationNode`
- `state`: `SUCCESS`, `ACCESSIBILITY_DISABLED`, `NO_ACTIVE_WINDOW`, `ROOT_NODE_UNAVAILABLE`, `OBSERVATION_FAILED`, `CANCELLED`

## 5. Phase 3 Test Isolation
All Phase 3.1 observation tests use the namespace `P3.1-OBS-001` through `P3.1-OBS-025`. Phase 2 foundation test suite and results remain strictly frozen and independent.
