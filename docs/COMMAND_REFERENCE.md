# LocalAgent Canonical Command & Capability Reference

This document is the **single canonical source of truth** for all commands and capabilities currently implemented and executable in LocalAgent.

---

## Command Inventory Summary

- **Total Registered Commands:** 125
- **Implemented Production Commands:** 104
- **Diagnostic Commands:** 13
- **Test-Only Commands:** 8
- **Test Registry Coverage:** 100% (229 test cases in `FoundationTestRegistry`)

---

## Standard Status Categories

- **`IMPLEMENTED`**: Fully implemented in production code and universally executable without hardware/OEM dependencies.
- **`IMPLEMENTED_DEVICE_DEPENDENT`**: Implemented in production code, but physical execution depends on Android OS version, OEM vendor, hardware availability, permission state, or foreground application state.
- **`IMPLEMENTED_DIAGNOSTIC`**: Implemented for developer console inspection, system diagnostics, or readiness evaluation.
- **`TEST_ONLY`**: Harness entry point created specifically for automated or real-device test execution.
- **`PARTIAL`**: Partial execution exists; pending full phase completion.
- **`PLANNED`**: Future roadmap concept not yet implemented in codebase.

---

## Phase 3.2 Real Action Execution Pipeline

All Phase 3.2 UI action commands (`back`, `recents`, `home`, `click`, `long click`, `text input`, `scroll forward`, `scroll backward`) route through a single unified execution pipeline:

```
User Console / Voice / Test Harness
              │
              ▼
       CommandRegistry
              │
              ▼
       GoalDispatcherImpl
              │
              ▼
       UiActionRequest
              │
              ▼
   Target & Precondition Validation (UiTargetValidator)
              │
              ▼
       UiActionExecutor
              │
              ▼
   LocalAgentAccessibilityService (Live Singleton)
              │
              ▼
   Android Accessibility API (performGlobalAction / performAction)
              │
              ▼
       Physical UI State Change
              │
              ▼
   Post-Action Observation & Verification (AccessibilityObservationEngine)
              │
              ▼
   Structured UiActionResult (SUCCESS / BLOCKED / FAILED / ACCESSIBILITY_UNAVAILABLE / TARGET_NOT_FOUND)
```

---

## Complete Command Inventory

### 1. Safety & Execution
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `safety.status` | `safety status` | `IMPLEMENTED_DIAGNOSTIC` | Queries execution controller lock state and current safety state machine status | `ExecutionController` |
| `safety.cancel` | `safety cancel` | `IMPLEMENTED` | Requests immediate cancellation of active execution coroutine jobs | `ExecutionController` |
| `safety.panic` | `panic` | `IMPLEMENTED_DEVICE_DEPENDENT` | Triggers emergency stop panic flow, releases execution lock, and issues `GLOBAL_ACTION_HOME` | `LocalAgentAccessibilityService` |

### 2. Headless Core - Calculator & Notes
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `calculator.calculate` | `calculate <expression>` | `IMPLEMENTED` | Evaluates arithmetic math expressions (+, -, *, /, ^, %, parentheses, decimals) | `CalculatorSkill` |
| `notes.append` | `note down <text>` | `IMPLEMENTED` | Appends text entry with timestamp to persistent local note file on storage | `NotesSkill` |

### 3. Intent Skills - Timer, Alarm, Web Search
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `timer.create` | `timer <seconds>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Launches system Clock timer intent for specified seconds | `IntentSkills` |
| `alarm.create` | `alarm <time>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Launches system Clock alarm intent for HH:MM time | `IntentSkills` |
| `web.search` | `web search <query>`, `search <query>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Dispatches `ACTION_WEB_SEARCH` intent for query string | `IntentSkills` |

### 4. Application Launching & Discovery
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `app.launch` | `open <app_name>`, `launch <app_name>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Resolves installed application query and launches application | `AppLauncherImpl` |
| `app.current` | `app current` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries active foreground application package name using `UsageStatsManager` | `UsageStatsController` |
| `app.list` | `app list` | `IMPLEMENTED_DEVICE_DEPENDENT` | Lists all launchable installed application names and package identifiers | `AppDiscoveryController` |
| `app.find` | `app find <query>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Searches installed applications matching query string | `AppDiscoveryController` |
| `app.info` | `app info <package>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries version name, version code, and target SDK for specified package | `AppDiscoveryController` |

### 5. Multi-Torch Flashlight
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `flashlight.status` | `flashlight status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries camera torch hardware availability and multi-torch mapping | `FlashlightController` |
| `flashlight.on` | `flashlight on`, `flashlight back` | `IMPLEMENTED_DEVICE_DEPENDENT` | Turns default/back camera torch light ON | `FlashlightController` |
| `flashlight.front` | `flashlight front` | `IMPLEMENTED_DEVICE_DEPENDENT` | Turns front camera torch light ON if present | `FlashlightController` |
| `flashlight.back` | `flashlight back` | `IMPLEMENTED_DEVICE_DEPENDENT` | Turns back camera torch light ON | `FlashlightController` |
| `flashlight.both` | `flashlight both` | `IMPLEMENTED_DEVICE_DEPENDENT` | Turns both front and back camera torches ON simultaneously | `FlashlightController` |
| `flashlight.off` | `flashlight off` | `IMPLEMENTED_DEVICE_DEPENDENT` | Turns all camera torch lights OFF | `FlashlightController` |
| `flashlight.target` | `flashlight <target>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Sets flashlight target dynamically (back, front, both, off, status) | `FlashlightController` |

### 6. Haptics & Vibration
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `haptics.status` | `vibrate status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries device vibrator motor availability and amplitude support | `HapticController` |
| `haptics.vibrate` | `vibrate <ms>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Triggers vibration motor for duration within 1..2000ms bounds | `HapticController` |

### 7. Audio Volume Streams (Music, Ring, Alarm, Notification)
For each audio stream (`music`, `ring`, `alarm`, `notification`), 5 commands are provided:
- `volume.<stream>.status` (`IMPLEMENTED_DEVICE_DEPENDENT`): Queries status, current index, max index, and percentage.
- `volume.<stream>.current` (`IMPLEMENTED_DEVICE_DEPENDENT`): Queries current volume index integer.
- `volume.<stream>.maximum` (`IMPLEMENTED_DEVICE_DEPENDENT`): Queries maximum volume index integer.
- `volume.<stream>.percentage` (`IMPLEMENTED_DEVICE_DEPENDENT`): Queries current volume percentage (0-100%).
- `volume.<stream>.set` (`IMPLEMENTED_DEVICE_DEPENDENT`): Sets volume percentage (0-100%) with read-after-write verification.

### 8. Connectivity (Wi-Fi & Bluetooth)
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `wifi.status` | `wifi status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries Wi-Fi adapter enabled/disabled state | `ConnectivityControllers` |
| `wifi.on` | `wifi on` | `IMPLEMENTED_DEVICE_DEPENDENT` | Requests Wi-Fi enablement | `ConnectivityControllers` |
| `wifi.off` | `wifi off` | `IMPLEMENTED_DEVICE_DEPENDENT` | Requests Wi-Fi disablement | `ConnectivityControllers` |
| `bluetooth.status` | `bluetooth status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries Bluetooth adapter state and hardware availability | `ConnectivityControllers` |
| `bluetooth.on` | `bluetooth on` | `IMPLEMENTED_DEVICE_DEPENDENT` | Requests Bluetooth toggle; opens Settings fallback on API 27+ | `ConnectivityControllers` |
| `bluetooth.off` | `bluetooth off` | `IMPLEMENTED_DEVICE_DEPENDENT` | Requests Bluetooth toggle; opens Settings fallback on API 27+ | `ConnectivityControllers` |

### 9. Clipboard & Notifications
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `clipboard.status` | `clipboard status` | `IMPLEMENTED` | Queries system clipboard service state | `ClipboardController` |
| `clipboard.read` | `clipboard read` | `IMPLEMENTED` | Reads text from clipboard | `ClipboardController` |
| `clipboard.write` | `clipboard write <text>` | `IMPLEMENTED` | Writes text to clipboard | `ClipboardController` |
| `clipboard.clear` | `clipboard clear` | `IMPLEMENTED` | Clears system clipboard contents | `ClipboardController` |
| `notification.status` | `notification status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries NotificationListenerService access and connection status | `NotificationController` |
| `notification.latest` | `notification latest` | `IMPLEMENTED_DEVICE_DEPENDENT` | Reads latest received notification snapshot | `NotificationController` |

### 10. Display, Input & Camera
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `display.status` | `display status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries screen metrics, density, and orientation | `DisplayController` |
| `display.dimensions` | `display dimensions` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries screen pixel width and height | `DisplayController` |
| `display.orientation` | `display orientation` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries screen orientation state | `DisplayController` |
| `screen.capture.status` | `screen capture status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries screen capture capability state | `ScreenCaptureController` |
| `keyboard.status` | `keyboard status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries soft keyboard visibility state | `InputStateController` |
| `input.status` | `input status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries IME input method status | `InputStateController` |
| `camera.status` | `camera status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries camera hardware availability and count | `CameraController` |
| `camera.permission` | `camera permission` | `IMPLEMENTED_DIAGNOSTIC` | Queries CAMERA runtime permission status | `CameraController` |
| `camera.list` | `camera list` | `IMPLEMENTED_DEVICE_DEPENDENT` | Lists installed camera device IDs | `CameraController` |

### 11. Network, Location, Power, Background & Unified Snapshot
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `network.status` | `network status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries active network connection status | `NetworkController` |
| `location.status` | `location status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries GPS and Network provider enablement states | `SystemControlControllers` |
| `location.providers` | `location providers` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries location provider details | `LocationController` |
| `battery.status` | `battery status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries battery level % and charging state | `SystemControlControllers` |
| `power.status` | `power status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries screen interactivity power state | `PowerStateController` |
| `background.policy` | `background policy` | `IMPLEMENTED_DIAGNOSTIC` | Queries background execution policy | `BackgroundExecutionPolicy` |
| `device.snapshot` | `device snapshot` | `IMPLEMENTED_DIAGNOSTIC` | Aggregates unified device state snapshot | `DeviceStateController` |

### 12. Dynamic Sensors
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `sensor.list` | `sensor list` | `IMPLEMENTED_DEVICE_DEPENDENT` | Enumerates hardware sensors via SensorManager | `HardwareObservationControllers` |
| `sensor.status` | `sensor status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries overall hardware sensor discovery status | `HardwareObservationControllers` |
| `sensor.info` | `sensor info <type>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries metadata info for specified sensor type | `HardwareObservationControllers` |
| `sensor.test` | `sensor test <type>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Tests registration and event receipt for a sensor | `HardwareObservationControllers` |
| `sensor.sample` | `sensor sample <type>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Samples values from specified sensor type | `HardwareObservationControllers` |
| `sensor.discovery` | `sensor discovery` | `IMPLEMENTED_DEVICE_DEPENDENT` | Discovers all SensorManager.TYPE_ALL sensors | `HardwareObservationControllers` |
| `sensor.accelerometer.sample` | `sensor accelerometer` | `IMPLEMENTED_DEVICE_DEPENDENT` | Samples 3-axis accelerometer values | `HardwareObservationControllers` |
| `sensor.gyroscope.sample` | `sensor gyroscope` | `IMPLEMENTED_DEVICE_DEPENDENT` | Samples 3-axis gyroscope values | `HardwareObservationControllers` |
| `sensor.proximity.sample` | `sensor proximity` | `IMPLEMENTED_DEVICE_DEPENDENT` | Samples proximity sensor distance (cm) with range metadata | `HardwareObservationControllers` |
| `sensor.light.sample` | `sensor light` | `IMPLEMENTED_DEVICE_DEPENDENT` | Samples ambient light illuminance (lux) | `HardwareObservationControllers` |

### 13. System Controls & Speech (STT/TTS)
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `brightness.status` | `brightness status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Reads current screen brightness level | `SystemControlControllers` |
| `brightness.set` | `brightness <pct>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Sets brightness level (0-100%) requiring `WRITE_SETTINGS` | `SystemControlControllers` |
| `ringer.status` | `ringer status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries current ringer mode | `SystemControlControllers` |
| `ringer.normal` | `ringer normal` | `IMPLEMENTED_DEVICE_DEPENDENT` | Sets ringer mode to NORMAL | `SystemControlControllers` |
| `ringer.vibrate` | `ringer vibrate` | `IMPLEMENTED_DEVICE_DEPENDENT` | Sets ringer mode to VIBRATE | `SystemControlControllers` |
| `ringer.silent` | `ringer silent` | `IMPLEMENTED_DEVICE_DEPENDENT` | Sets ringer mode to SILENT requiring Do Not Disturb Access | `SystemControlControllers` |
| `stt.status` | `stt status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries SpeechRecognizer availability | `SpeechToTextEngine` |
| `stt.listen` | `stt listen` | `IMPLEMENTED_DEVICE_DEPENDENT` | Starts STT speech recognition listener requiring `RECORD_AUDIO` | `SpeechToTextEngine` |
| `stt.cancel` | `stt cancel` | `IMPLEMENTED_DEVICE_DEPENDENT` | Cancels active speech recognition session | `SpeechToTextEngine` |
| `tts.status` | `tts status` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries TextToSpeech engine status | `TextToSpeechEngine` |
| `tts.speak` | `speak <text>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Speaks text using native TTS | `TextToSpeechEngine` |
| `tts.stop` | `tts stop` | `IMPLEMENTED_DEVICE_DEPENDENT` | Stops active speech output | `TextToSpeechEngine` |

### 14. Target Resolution & Real UI Actions
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `target.resolve` | `target resolve <query>` | `IMPLEMENTED` | Resolves target UI node from active ObservationSnapshot | `TargetResolver` |
| `target.find` | `target find <query>` | `IMPLEMENTED` | Finds target candidate nodes matching query | `TargetResolver` |
| `target.inspect` | `target inspect <node_id>` | `IMPLEMENTED` | Inspects node metadata and actionability properties | `TargetResolver` |
| `target.candidates` | `target candidates <query>` | `IMPLEMENTED` | Lists all ranked candidate nodes for query | `TargetResolver` |
| `action.click` | `action click <query>`, `click <query>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Executes CLICK action on resolved UI target node | `UiActionExecutor` |
| `action.long_click` | `action long_click <query>`, `long click <query>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Executes LONG_CLICK action on resolved UI target node | `UiActionExecutor` |
| `action.input` | `action input <text>`, `text input <text>` | `IMPLEMENTED_DEVICE_DEPENDENT` | Executes TEXT_INPUT action on resolved editable target node | `UiActionExecutor` |
| `action.scroll` | `action scroll <direction>`, `scroll forward`, `scroll backward` | `IMPLEMENTED_DEVICE_DEPENDENT` | Executes SCROLL action on resolved scrollable target node | `UiActionExecutor` |
| `action.back` | `action back`, `back` | `IMPLEMENTED_DEVICE_DEPENDENT` | Executes `GLOBAL_ACTION_BACK` navigation action | `UiActionExecutor` |
| `action.recents` | `action recents`, `recents` | `IMPLEMENTED_DEVICE_DEPENDENT` | Executes `GLOBAL_ACTION_RECENTS` navigation action | `UiActionExecutor` |
| `action.home` | `action home`, `home` | `IMPLEMENTED_DEVICE_DEPENDENT` | Executes `GLOBAL_ACTION_HOME` navigation action | `UiActionExecutor` |
| `action.status` | `action status` | `IMPLEMENTED_DIAGNOSTIC` | Queries action subsystem readiness and connection status | `UiActionExecutor` |

### 15. Movable Action Overlay
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `overlay.show` | `overlay show` | `IMPLEMENTED_DEVICE_DEPENDENT` | Launches or shows movable action overlay panel | `LocalAgentOverlayService` |
| `overlay.hide` | `overlay hide` | `IMPLEMENTED` | Hides movable action overlay panel | `LocalAgentOverlayService` |
| `overlay.status` | `overlay status` | `IMPLEMENTED_DIAGNOSTIC` | Queries movable action overlay status | `LocalAgentOverlayService` |

### 15.1 Persistent Logging Commands
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `logs.recent` | `logs recent` | `IMPLEMENTED_DIAGNOSTIC` | Queries recent persistent structured SQLite log records | `LocalAgentLogger` |
| `logs.errors` | `logs errors` | `IMPLEMENTED_DIAGNOSTIC` | Queries error log records from SQLite database | `LocalAgentLogger` |
| `logs.command` | `logs command <correlationId>` | `IMPLEMENTED_DIAGNOSTIC` | Queries log trace for a specific correlation ID | `LocalAgentLogger` |
| `logs.clear` | `logs clear` | `IMPLEMENTED_DIAGNOSTIC` | Clears persistent SQLite log database | `LocalAgentLogger` |

### 16. Discovery, Namespaced Observation & System Status
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `help` | `help <query>` | `IMPLEMENTED_DIAGNOSTIC` | Lists available command categories or detailed command help | `CommandRegistry` |
| `commands` | `commands <filter>` | `IMPLEMENTED_DIAGNOSTIC` | Lists all registered production commands | `CommandRegistry` |
| `observe.start` | `observe start` | `IMPLEMENTED_DEVICE_DEPENDENT` | Starts Accessibility Observation Mode | `AccessibilityObservationEngine` |
| `observe.stop` | `observe stop` | `IMPLEMENTED_DEVICE_DEPENDENT` | Stops Accessibility Observation Mode | `AccessibilityObservationEngine` |
| `observe.current` | `observe current` | `IMPLEMENTED_DEVICE_DEPENDENT` | Queries current foreground package and observation snapshot summary | `AccessibilityObservationEngine` |
| `observe.nodes` | `observe nodes` | `IMPLEMENTED_DEVICE_DEPENDENT` | Summarizes visible nodes in active observation snapshot | `AccessibilityObservationEngine` |
| `system.status` | `system status` | `IMPLEMENTED_DIAGNOSTIC` | Queries unified system readiness, capabilities, and service states | `FoundationReadinessEvaluator` |
| `ui.state` | `ui state` | `IMPLEMENTED_DIAGNOSTIC` | Queries central UI state and speech configuration | `AgentUiState` |

### 16. Test Harness Commands
| Command ID | Aliases | Status | Purpose | Handler |
|------------|---------|--------|---------|---------|
| `test.launch` | `test launch <target>` | `TEST_ONLY` | Launches test target app and waits for actual foreground package | `ControlledTestAppLauncher` |
| `test.observe` | `test observe` | `TEST_ONLY` | Captures current active UI observation snapshot | `AccessibilityObservationEngine` |
| `test.click` | `test click <query>` | `TEST_ONLY` | Executes test CLICK action against target | `UiActionExecutor` |
| `test.long_click` | `test long_click <query>` | `TEST_ONLY` | Executes test LONG_CLICK action against target | `UiActionExecutor` |
| `test.text_input` | `test text_input <text>` | `TEST_ONLY` | Executes test TEXT_INPUT action against target | `UiActionExecutor` |
| `test.scroll` | `test scroll <direction>` | `TEST_ONLY` | Executes test SCROLL action against target | `UiActionExecutor` |
| `test.back` | `test back` | `TEST_ONLY` | Executes test GLOBAL_BACK action | `UiActionExecutor` |
| `test.run` | `test run <test_id>` | `TEST_ONLY` | Executes complete automated Phase 3.2 real-device test scenario | `GuidedPhase32ActionRunner` |

---

## Command → Code Mapping Table

| Command ID | CommandRegistry Entry | GoalDispatcherImpl Route | Primary Executor Class | Android OS / API Used |
|------------|------------------------|--------------------------|------------------------|-----------------------|
| `back` / `action.back` | `action.back` | `dispatchAndProcessWithLock` | `UiActionExecutor` | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_BACK)` |
| `recents` / `action.recents` | `action.recents` | `dispatchAndProcessWithLock` | `UiActionExecutor` | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_RECENTS)` |
| `home` / `action.home` | `action.home` | `dispatchAndProcessWithLock` | `UiActionExecutor` | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_HOME)` |
| `click` / `action.click` | `action.click` | `dispatchAndProcessWithLock` | `UiActionExecutor` | `AccessibilityNodeInfo.performAction(ACTION_CLICK)` |
| `long click` / `action.long_click` | `action.long_click` | `dispatchAndProcessWithLock` | `UiActionExecutor` | `AccessibilityNodeInfo.performAction(ACTION_LONG_CLICK)` |
| `text input` / `action.input` | `action.input` | `dispatchAndProcessWithLock` | `UiActionExecutor` | `AccessibilityNodeInfo.performAction(ACTION_SET_TEXT, bundle)` |
| `scroll` / `action.scroll` | `action.scroll` | `dispatchAndProcessWithLock` | `UiActionExecutor` | `AccessibilityNodeInfo.performAction(ACTION_SCROLL_FORWARD / BACKWARD)` |
| `open <app>` / `app.launch` | `app.launch` | `dispatchAndProcessWithLock` | `AppLauncherImpl` | `PackageManager.getLaunchIntentForPackage` / `startActivity` |
| `flashlight on` | `flashlight.on` | `dispatchAndProcessWithLock` | `FlashlightController` | `CameraManager.setTorchMode` |
| `vibrate 200` | `haptics.vibrate` | `dispatchAndProcessWithLock` | `HapticController` | `Vibrator.vibrate(VibrationEffect)` |
| `volume music 50` | `volume.music.set` | `dispatchAndProcessWithLock` | `VolumeController` | `AudioManager.setStreamVolume` |
| `brightness 50` | `brightness.set` | `dispatchAndProcessWithLock` | `SystemControlControllers` | `Settings.System.putInt(SCREEN_BRIGHTNESS)` |
| `ringer normal` | `ringer.normal` | `dispatchAndProcessWithLock` | `SystemControlControllers` | `AudioManager.ringerMode` |
| `stt listen` | `stt.listen` | `dispatchAndProcessWithLock` | `SpeechToTextEngine` | `SpeechRecognizer.startListening(Intent)` |
| `speak <text>` | `tts.speak` | `dispatchAndProcessWithLock` | `TextToSpeechEngine` | `TextToSpeech.speak(text, QUEUE_FLUSH)` |

---

## Test Coverage Matrix

| Command Category | Command IDs | Unit Test Suite | Integration Test Suite | Device Verified | Status |
|------------------|-------------|-----------------|------------------------|-----------------|--------|
| Safety & Execution | `safety.*` | `Phase1SafetyUnitTest` | `Phase1SafetyTestHarness` | YES | `IMPLEMENTED` |
| Headless Core | `calculator.*`, `notes.*`, `timer.*`, `alarm.*`, `web.*` | `Phase2HeadlessCoreUnitTest` | `FoundationTestRegistry` | YES | `IMPLEMENTED` |
| App Discovery & Launch | `app.*` | `Phase2AppLaunchUnitTest` | `FoundationTestRegistry` | YES | `IMPLEMENTED` |
| Flashlight | `flashlight.*` | `Phase2FlashlightUnitTest` | `FoundationTestRegistry` | YES | `IMPLEMENTED` |
| Haptics | `haptics.*` | `Phase2HapticsUnitTest` | `FoundationTestRegistry` | YES | `IMPLEMENTED` |
| Volume Control | `volume.*` | `Phase2VolumeUnitTest` | `FoundationTestRegistry` | YES | `IMPLEMENTED` |
| Connectivity | `wifi.*`, `bluetooth.*` | `Phase2ConnectivityUnitTest` | `FoundationTestRegistry` | YES | `IMPLEMENTED` |
| Clipboard & Notifications | `clipboard.*`, `notification.*` | `Phase25FoundationUnitTest` | `FoundationTestRegistry` | YES | `IMPLEMENTED` |
| Sensors | `sensor.*` | `Phase2HardeningUnitTest` | `FoundationTestRegistry` | YES | `IMPLEMENTED` |
| System Controls | `brightness.*`, `ringer.*`, `location.*`, `battery.*` | `Phase2HardeningUnitTest` | `FoundationTestRegistry` | YES | `IMPLEMENTED` |
| Speech | `stt.*`, `tts.*` | `SpeechSubsystemUnitTest` | `FoundationTestRegistry` | YES | `IMPLEMENTED` |
| Target Resolution | `target.*` | `Phase32TargetResolutionUnitTest` | `FoundationTestRegistry` | YES | `IMPLEMENTED` |
| Real Action Execution | `action.*`, `back`, `recents`, `home` | `Phase32ActionExecutionUnitTest` | `FoundationTestRegistry` (`P3.2-ACT-001`..`025`) | YES | `IMPLEMENTED` |
| Command Discovery | `help`, `commands`, `observe.*`, `system.*`, `ui.*` | `CommandRegistryAuditUnitTest` | `FoundationTestRegistry` | YES | `IMPLEMENTED` |

---

## Roadmap Concepts (Future Planned)

The following capabilities are **Future Concepts** and are **NOT** listed as current commands:
- Autonomous Multi-Step Planning & LLM Agent
- Multi-step goal decomposition engine
- Expense tracking & financial utility
- YouTube analytics utility
- Trip planning utility
- Sudoku / Game solver
- Always-on wake-word voice detection
- Computer Vision / OCR screen understanding
