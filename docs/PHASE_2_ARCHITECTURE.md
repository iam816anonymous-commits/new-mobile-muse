# Phase 2 Architecture: Deterministic Headless Core & Device Control

## System Architecture Overview

Phase 2 enables LocalAgent to perform deterministic tasks and hardware control without accessibility UI automation or autonomous planning.

```text
Explicit Goal Command
        │
        ▼
   GoalDispatcherImpl  ◄────── Command Parsing (CALCULATE, NOTE, TIMER, ALARM, WEB_SEARCH, DEVICE)
        │
        ▼
ExecutionController   ◄────── Try / Finally Execution Ownership Lock
        │
        ▼
MasterWatchdog & Safety ◄──── Central Cancellation & 6000ms Timeout Budget
        │
        ├──► Headless Skills
        │     ├── CalculatorSkill      (In-memory RPN expression evaluator)
        │     ├── NotesSkill           (Shared storage line appender)
        │     └── IntentSkills         (Native AlarmClock & Web Search Intents)
        │
        └──► Device Controllers
              ├── FlashlightController (CameraManager Torch API)
              ├── HapticController     (Vibrator API with duration limits)
              ├── VolumeController     (AudioManager stream control)
              ├── ConnectivityControllers (WifiManager & BluetoothAdapter state)
              └── HardwareObservationControllers (Battery broadcast & on-demand sensor sampling)
```

## Architectural Safeguards

1. **No Accessibility UI Automation**:
   Skills and device controllers use direct system APIs or platform Intents. `LocalAgentAccessibilityService.onAccessibilityEvent()` remains observation-only.
2. **Single Goal Execution**:
   All operations pass through `ExecutionController` lock to maintain `ONE GOAL EXECUTION AT A TIME`.
3. **Structured Results**:
   Every skill returns a `SkillResult` with `SkillStatus` (`SUCCESS`, `FAILED`, `PERMISSION_REQUIRED`, `UNSUPPORTED`, `UNAVAILABLE`, `CANCELLED`, `INVALID_GOAL`).
4. **Mandatory Resource Unregistration**:
   Sensor sampling automatically unregisters listeners upon completion or timeout using `finally` blocks.
