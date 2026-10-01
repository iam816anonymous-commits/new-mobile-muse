# Phase 3.1 — Observation Foundation & Cross-App Architecture

## 1. Overview & Objective
Phase 3.1 establishes the perception/observation foundation for LocalAgent on Android 8.1 / API 27 targets. The observation subsystem inspects currently visible user interface hierarchies via native Android `AccessibilityService` APIs and produces structured, immutable, machine-readable representations (`ObservationSnapshot` / `ObservationNode`).

## 2. Cross-Application Observation Mode
LocalAgent supports background cross-application observation:
1. User starts Observation Mode (`observationEngine.startObservationMode()`).
2. User leaves LocalAgent and opens another Android application (e.g. Android Settings).
3. `LocalAgentAccessibilityService.onAccessibilityEvent()` detects foreground window changes and passes debounced events (min 1000ms interval) to `AccessibilityObservationEngine`.
4. `AccessibilityObservationEngine` captures the external application's UI hierarchy and stores it in `lastExternalSnapshot`.
5. User returns to LocalAgent and inspects the external application snapshot.
6. The entire pipeline remains strictly **READ-ONLY**.

## 3. Core Architectural Rules & Safety
1. **STRICTLY READ-ONLY**: The Observation Engine (`AccessibilityObservationEngine`) performs zero UI actions, gestures, taps, clicks, text input, or app launches.
2. **DEBOUNCED CAPTURE**: Capture events are debounced with a 1000ms minimum interval (`DEBOUNCE_INTERVAL_MS = 1000L`) to prevent event storms, race conditions, memory leaks, or ANRs.
3. **ZERO RECURSIVE LEAKS**: All `AccessibilityNodeInfo` instances are recycled (`nodeInfo.recycle()`) during traversal.
4. **TRAVERSAL BOUNDS**: Maximum node count is strictly capped at `500` nodes and depth is capped at `30` levels to protect target Tecno Camon i (4GB RAM) hardware.

## 4. Architecture Component Diagram
```
External App / Foreground Window Change
             ↓
LocalAgentAccessibilityService (onAccessibilityEvent)
             ↓
AccessibilityObservationEngine (Debounced 1000ms -> rootInActiveWindow)
             ↓
ObservationSnapshot / ObservationNode (Immutable Data Models)
             ↓
JSON Serialization / Phase 3.1 UI Inspector / Target Resolver
```

## 5. Phase 3 Test Isolation
- **Core Observation Tests**: `P3.1-OBS-001` through `P3.1-OBS-025`
- **Cross-App Validation Tests**: `P3.1-XAPP-001` through `P3.1-XAPP-008`
- **Evidence Storage**: `evidence/phase3.1/xapp/TEST-P3.1-XAPP-*/`
- All Phase 2 foundation tests and evidence remain strictly frozen and independent.
