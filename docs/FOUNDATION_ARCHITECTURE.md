# LocalAgent Foundation Architecture (Phase 2.4)

## Architecture Overview
LocalAgent Phase 2.4 establishes the complete foundation layer required prior to Phase 3 (LLM / Autonomous AI Behavior). The architecture enforces deterministic, single-action execution ownership, hardware controls, safety boundaries, and manual/automated test verification.

```
+-------------------------------------------------------------------+
|                        MainActivity UI                            |
|  [Test Runner]  [Permissions]  [Diagnostics]  [Live Console]      |
+-------------------------------------------------------------------+
                                 |
                                 v
                     +-----------------------+
                     |    GoalDispatcher     |
                     +-----------------------+
                                 |
                                 v
                     +-----------------------+
                     |  ExecutionController  |
                     |  - StateMachine       |
                     |  - CancellationMgr    |
                     |  - MasterWatchdog     |
                     +-----------------------+
                                 |
         +-----------------------+-----------------------+
         |                       |                       |
         v                       v                       v
+------------------+   +-------------------+   +--------------------+
|  Device Skills   |   | System Controllers|   | STT / TTS Engines  |
|  - Calculator    |   | - Flashlight      |   | - SpeechRecognizer |
|  - Notes         |   | - Haptics         |   | - TextToSpeech     |
|  - Intents       |   | - Volume          |   +--------------------+
|  - AppLauncher   |   | - Connectivity    |
+------------------+   | - Observation     |
                       | - SystemControls  |
                       +-------------------+
```

## Architectural Boundaries & Safety Directives
1. **Observation-Only Accessibility Service**: `LocalAgentAccessibilityService.onAccessibilityEvent()` is observation-only and MUST NEVER dispatch actions, perform autonomous gestures, or launch applications.
2. **Key Filtering & Volume Key Safety**: Single volume key presses (`KEYCODE_VOLUME_UP`, `KEYCODE_VOLUME_DOWN`, `KEYCODE_VOLUME_MUTE`) return `false` immediately to preserve native Android volume propagation. Only a double Volume-Up press within 500ms triggers `USER_PANIC` emergency stop.
3. **Deterministic Single-Execution Lock**: `ExecutionController` enforces a strict one-action-at-a-time policy. Concurrent executions are rejected.
4. **MasterWatchdog Timeout Policy**: Bounded execution timeout (default 6000ms) guarantees job cancellation and state recovery if an action hangs.
