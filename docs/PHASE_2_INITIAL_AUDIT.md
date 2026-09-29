# Phase 2 Initial Repository Audit

**Date**: September 2026
**Target Hardware**: Tecno Camon i (Android 8.1 / API 27, MediaTek Helio P23, 4 GB RAM)

## Existing Phase 1 Architecture Inspection

| Subsystem | Existing Implementation | Phase 2 Extension Points |
| :--- | :--- | :--- |
| **Execution Boundary** | `GoalDispatcher.kt` interface. | Implement `GoalDispatcherImpl` and `CommandParser` to route deterministic goals (`CALCULATE`, `NOTE`, `SET_TIMER`, `SET_ALARM`, `WEB_SEARCH`, device commands). |
| **Execution Ownership** | `ExecutionController.kt`. | Wrap every Phase 2 skill & device operation in `acquireExecution` / `releaseExecution` with `finally` locks. |
| **Central Safety** | `CentralCancellationManager` & `MasterWatchdog`. | Attach 6000ms watchdog budget & cancel active skill execution jobs if deadline is exceeded. |
| **Storage Architecture** | `LearningStore.kt` & `Logger.kt`. | Add `NotesStore` writing `[yyyy-MM-dd HH:mm] content` to shared Downloads or fallback storage without unbounded memory lists. |
| **Accessibility Service** | `LocalAgentAccessibilityService.kt`. | Intact & observation-only. Double Volume-Up hardware panic button triggers emergency stop. |
| **Control Plane UI** | `MainActivity.kt` & `activity_main.xml`. | Extend UI with Phase 2 Headless Core Panel, Device Control Panel, and Physical Verification Section. |

## Android API 27 Risks & Compatibility Safeguards

* **Direct Connectivity Toggling**: Direct Wi-Fi or Bluetooth state changes may require API-specific permissions or system settings fallbacks. `UNSUPPORTED_DIRECT_CONTROL` status will be returned rather than attempting undocumented hacks.
* **Storage Permissions**: Writing notes to shared storage requires runtime `WRITE_EXTERNAL_STORAGE` permission on API 27. Missing permissions will safely return `PERMISSION_REQUIRED` without crashing.
* **Sensor Resource Footprint**: On-demand sensor sampling requires mandatory listener unregistration in `finally` blocks and timeout cancellation to prevent battery and memory leaks.
* **No UI Automation**: Accessibility node crawling, coordinate tapping, shell inputs, ADB commands, and root hacks remain strictly prohibited.
