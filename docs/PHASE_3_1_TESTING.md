# Phase 3.1 — Testing & Verification Guide

## 1. Test Architecture Overview
Phase 3.1 testing is split into two complementary tracks:
1. **Automated Unit Tests**: Unit tests verifying node models, snapshot stores, window classification, and runner state machines.
2. **Interactive UI Diagnostics & Harness**: Two dedicated cards in `MainActivity` Diagnostics tab (`Card A` and `Card B`) plus a sequential test runner.

## 2. Diagnostics UI Validation Cards

### Card A — Observation Engine Validation (Current Screen)
- **Button**: `[ RUN ENGINE VALIDATION ]`
- **Purpose**: Evaluates all 25 core observation engine criteria on the currently active LocalAgent screen.
- **Output**: Reports PASS/FAIL along with package, activity, node count, duration (ms), and tree bounds.

### Card B — Guided External Observation Test (`GuidedExternalObservationRunner`)
- **Target App Selector**: `Chrome` (`P3.1-EXT-001`), `YouTube` (`P3.1-EXT-002`), `Settings` (`P3.1-EXT-003`), `Calculator` (`P3.1-EXT-004`).
- **Button**: `[ START GUIDED TEST ]`
- **Flow**:
  1. Resolves launch intent for target app.
  2. Launches target app.
  3. Polls foreground window (15s timeout) waiting for target package.
  4. Captures and validates UI hierarchy (7-check checklist).
  5. Preserves snapshot in `ObservationSnapshotStore`.
  6. Saves evidence JSON to `evidence/phase3.1/guided_external_<target>.json`.
  7. When user returns to LocalAgent, snapshot remains preserved and displayed.

## 3. Automated Test Suites
- **`Phase31ObservationUnitTest`**: Tests `ObservationNode` JSON serialization, `ObservationSnapshot` JSON serialization, null accessibility service safeguards, and Phase 3.1 test case registry isolation.
- **`Phase31CrossAppObservationUnitTest`**: Tests `ExternalAppTestValidator`, package exclusion rules, `ObservationSnapshotStore` session token invalidation, `stopObservationMode()` snapshot preservation, LocalAgent overwrite prevention, window classification logic, `ObservedWindow` JSON serialization, and `AccessibleWindowProvider`.

## 4. Sequential Manual Verification Protocol
Physical device verification must be performed **ONE TEST AT A TIME**:
1. **Chrome Test (`P3.1-EXT-001`)**: Select `Chrome` -> Tap `START GUIDED TEST` -> Chrome launches -> Target detected -> Capture & Validate -> Return to LocalAgent -> Confirm `com.android.chrome` snapshot remains preserved.
2. **YouTube Test (`P3.1-EXT-002`)**: Select `YouTube` -> Tap `START GUIDED TEST` -> YouTube launches -> Target detected -> Capture & Validate -> Return to LocalAgent -> Confirm `com.google.android.youtube` snapshot remains preserved.
3. **Settings Test (`P3.1-EXT-003`)**: Select `Settings` -> Tap `START GUIDED TEST` -> Settings launches -> Target detected -> Capture & Validate -> Return to LocalAgent -> Confirm `com.android.settings` snapshot remains preserved.
4. **Calculator Test (`P3.1-EXT-004`)**: Select `Calculator` -> Tap `START GUIDED TEST` -> Calculator launches -> Target detected -> Capture & Validate -> Return to LocalAgent -> Confirm calculator package snapshot remains preserved.
