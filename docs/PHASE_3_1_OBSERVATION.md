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
2. **UNIVERSAL OBSERVATION ARCHITECTURE**: The observation engine captures whatever accessibility information Android API 27 exposes without hardcoding package drop rules at the raw capture level.
3. **WINDOW CLASSIFICATION (`WindowClassification`)**: Every observed snapshot classifies its target window (`APPLICATION`, `LOCAL_AGENT`, `SYSTEM_UI`, `LAUNCHER`, `RECENTS`, `SETTINGS`, `NOTIFICATION_SURFACE`, `QUICK_SETTINGS`, `SYSTEM_DIALOG`, `OVERLAY`, `UNKNOWN`).
4. **INTERACTIVE WINDOW DISCOVERY (`AccessibleWindowProvider`)**: Uses `AccessibilityService.windows` (enabled via `flagRetrieveInteractiveWindows` API 21+) for multi-window discovery with active root fallback on API 27.
5. **TARGET POLICY SEPARATION**: Raw observation records exposed windows universally. Target policy resolution (`ExternalAppTestValidator` / `GuidedExternalObservationRunner`) evaluates whether an observed window matches the expected target task.
6. **CANONICAL SNAPSHOT STORE (`ObservationSnapshotStore`)**: Centralized single source of truth managing `currentLiveSnapshot`, `lastValidExternalSnapshot`, `displayedSnapshot`, `observationMode`, and session token invalidation (`sessionId`).
7. **STOP DOES NOT CAPTURE**: Tapping `STOP OBSERVATION` or returning to LocalAgent stops future capture and invalidates background sessions without invoking `rootInActiveWindow` or overwriting `lastValidExternalSnapshot`.
8. **ZERO RECURSIVE LEAKS**: All `AccessibilityNodeInfo` instances are recycled (`nodeInfo.recycle()`) during traversal.
9. **TRAVERSAL BOUNDS**: Maximum node count is strictly capped at `500` nodes and depth is capped at `30` levels to protect target Tecno Camon i (4GB RAM) hardware.

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

## 5. Dedicated Phase 3.1 UI Validation Cards
The Diagnostics UI contains two distinct validation cards powered by `ExternalAppTestValidator`:

### Card A: Observation Engine Validation (Current Screen)
- **Button**: `[ RUN ENGINE VALIDATION ]`
- **Execution**: Evaluates all 25 observation engine unit/integration criteria on the currently active screen.
- **Reporting**: Displays PASS/FAIL status along with target package, activity, total node count, duration (ms), and tree bounds.

### Card B: Guided External Observation Test (`GuidedExternalObservationRunner`)
- **Target Application Selector**: `Chrome` (`P3.1-EXT-001`), `YouTube` (`P3.1-EXT-002`), `Settings` (`P3.1-EXT-003`), `Calculator` (`P3.1-EXT-004`).
- **State Machine Workflow**:
  1. `PREPARING`: Resolves package name and verifies launch intent.
  2. `LAUNCHING`: Launches single selected target application via Intent.
  3. `WAITING_FOR_FOREGROUND`: Polls foreground package every 500ms (15s timeout), ignoring LocalAgent, System UI, and Launcher.
  4. `TARGET_DETECTED`: Confirms target foreground state.
  5. `CAPTURING`: Captures external application UI tree hierarchy.
  6. `VALIDATING`: Evaluates 7-point validation checklist.
  7. `PRESERVING`: Stores snapshot in `lastExternalSnapshot` and saves evidence JSON to `evidence/phase3.1/guided_external_<target>.json`.
  8. `COMPLETED`: Displays final PASSED/FAILED result card.
- **Snapshot Overwrite Protection**: `AccessibilityObservationEngine.isExcludedExternalPackage()` ensures LocalAgent (`com.agent.android`), System UI (`com.android.systemui`), launchers, and recents never overwrite `lastExternalSnapshot` when returning to LocalAgent or pressing STOP.

## 6. System Surface Tests & Non-Exposed Surface Handling
Phase 3.1 includes dedicated system surface tests:
- `P3.1-SYS-LOCAL-001`: LocalAgent Self-Observation.
- `P3.1-SYS-LAUNCHER-001`: Home / Launcher Observation (returns PASS or `SUPPORTED_SURFACE_BUT_NOT_EXPOSED` if Android/OEM launcher exposes no accessibility tree).
- `P3.1-SYS-RECENTS-001`: Recents Screen Observation (`RECENTS` classification).
- `P3.1-SYS-SYSTEMUI-001`: System UI Surface Observation (`SYSTEM_UI` classification).

If an Android surface is foreground but Android exposes no accessibility root node, the Observation Engine returns `SUPPORTED_SURFACE_BUT_NOT_EXPOSED` rather than treating it as an engine failure or fabricating fake nodes.

## 7. Phase 3 Test Isolation
- **Core Observation Tests**: `P3.1-OBS-001` through `P3.1-OBS-025`
- **Window Classification & System Surface Tests**: `P3.1-OBS-033` through `P3.1-OBS-039`, `P3.1-SYS-LOCAL-001` through `P3.1-SYS-SYSTEMUI-001`
- **Cross-App Validation Tests**: `P3.1-XAPP-001` through `P3.1-XAPP-008`
- **Guided External Observation Tests**: `P3.1-EXT-001` through `P3.1-EXT-004`
- **Evidence Storage**: `evidence/phase3.1/guided_external_<target>.json`
- All Phase 2 foundation tests and evidence remain strictly frozen and independent.
