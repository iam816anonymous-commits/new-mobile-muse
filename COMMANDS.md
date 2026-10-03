# LocalAgent Production Command Reference

This document is the **canonical root reference** for all **122 production commands** currently implemented and executable in LocalAgent across Phase 1, Phase 2, Phase 2.5, Phase 3.1, Phase 3.2, and System/UI subsystems.

For detailed execution pipeline traces, developer code mappings, and the complete test matrix, see [docs/COMMAND_REFERENCE.md](docs/COMMAND_REFERENCE.md).

---

## Command Coverage Overview

- **Total Registered Commands:** 122
- **Implemented Production Commands:** 101
- **Diagnostic Commands:** 13
- **Test-Only Commands:** 8
- **Test Registry Coverage:** 100% (229 test cases in `FoundationTestRegistry`)

---

## Implementation Status Legend

- **`IMPLEMENTED`**: Fully implemented in production code and universally executable without hardware/OEM dependencies.
- **`IMPLEMENTED_DEVICE_DEPENDENT`**: Implemented in production code, but physical execution depends on Android OS version, OEM vendor, hardware availability, permission state, or foreground application state.
- **`IMPLEMENTED_DIAGNOSTIC`**: Implemented for developer console inspection, system diagnostics, or readiness evaluation.
- **`TEST_ONLY`**: Harness entry point created specifically for automated or real-device test execution.

---

## 1. Safety & Execution (3 Commands)

1. **`safety.status`** (`IMPLEMENTED_DIAGNOSTIC`)
   - **Syntax:** `safety status`
   - **Purpose:** Queries execution controller lock state and safety state machine status.
   - **Handler:** `ExecutionController`

2. **`safety.cancel`** (`IMPLEMENTED`)
   - **Syntax:** `safety cancel`
   - **Purpose:** Requests immediate cancellation of active execution coroutine jobs.
   - **Handler:** `ExecutionController`

3. **`safety.panic`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
   - **Syntax:** `panic`
   - **Purpose:** Triggers emergency stop panic flow, releases execution lock, and issues `GLOBAL_ACTION_HOME`.
   - **Handler:** `LocalAgentAccessibilityService`

---

## 2. Headless Core - Calculator & Notes (2 Commands)

4. **`calculator.calculate`** (`IMPLEMENTED`)
   - **Syntax:** `calculate <expression>`
   - **Purpose:** Evaluates arithmetic math expressions (+, -, *, /, ^, %, parentheses, decimals).
   - **Handler:** `CalculatorSkill`

5. **`notes.append`** (`IMPLEMENTED`)
   - **Syntax:** `note down <text>`
   - **Purpose:** Appends text entry with timestamp to persistent local note file on storage.
   - **Handler:** `NotesSkill`

---

## 3. Intent Skills - Timer, Alarm, Web Search (3 Commands)

6. **`timer.create`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
   - **Syntax:** `timer <seconds>`
   - **Purpose:** Launches system Clock timer intent for specified seconds.
   - **Handler:** `IntentSkills`

7. **`alarm.create`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
   - **Syntax:** `alarm <time>`
   - **Purpose:** Launches system Clock alarm intent for HH:MM time.
   - **Handler:** `IntentSkills`

8. **`web.search`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
   - **Syntax:** `web search <query>`
   - **Purpose:** Dispatches `ACTION_WEB_SEARCH` intent for query string.
   - **Handler:** `IntentSkills`

---

## 4. Application Launching & Discovery (5 Commands)

9. **`app.launch`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
   - **Syntax:** `open <app_name>`
   - **Purpose:** Resolves installed application query and launches application.
   - **Handler:** `AppLauncherImpl`

10. **`app.current`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
    - **Syntax:** `app current`
    - **Purpose:** Queries active foreground application package name using `UsageStatsManager`.
    - **Handler:** `UsageStatsController`

11. **`app.list`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
    - **Syntax:** `app list`
    - **Purpose:** Lists all launchable installed application names and package identifiers.
    - **Handler:** `AppDiscoveryController`

12. **`app.find`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
    - **Syntax:** `app find <query>`
    - **Purpose:** Searches installed applications matching query string.
    - **Handler:** `AppDiscoveryController`

13. **`app.info`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
    - **Syntax:** `app info <package>`
    - **Purpose:** Queries version name, version code, and target SDK for specified package.
    - **Handler:** `AppDiscoveryController`

---

## 5. Multi-Torch Flashlight (7 Commands)

14. **`flashlight.status`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
15. **`flashlight.on`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
16. **`flashlight.front`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
17. **`flashlight.back`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
18. **`flashlight.both`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
19. **`flashlight.off`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
20. **`flashlight.target`** (`IMPLEMENTED_DEVICE_DEPENDENT`)

---

## 6. Haptics & Vibration (2 Commands)

21. **`haptics.status`** (`IMPLEMENTED_DEVICE_DEPENDENT`)
22. **`haptics.vibrate`** (`IMPLEMENTED_DEVICE_DEPENDENT`)

---

## 7. Audio Volume Streams (20 Commands)

For each audio stream (`music`, `ring`, `alarm`, `notification`):
- `volume.<stream>.status` (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `volume.<stream>.current` (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `volume.<stream>.maximum` (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `volume.<stream>.percentage` (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `volume.<stream>.set` (`IMPLEMENTED_DEVICE_DEPENDENT`)

---

## 8. Connectivity - Wi-Fi & Bluetooth (6 Commands)

- `wifi.status`, `wifi.on`, `wifi.off` (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `bluetooth.status`, `bluetooth.on`, `bluetooth.off` (`IMPLEMENTED_DEVICE_DEPENDENT`)

---

## 9. Clipboard & Notifications (6 Commands)

- `clipboard.status`, `clipboard.read`, `clipboard.write`, `clipboard.clear` (`IMPLEMENTED`)
- `notification.status`, `notification.latest` (`IMPLEMENTED_DEVICE_DEPENDENT`)

---

## 10. Display, Input & Camera (9 Commands)

- `display.status`, `display.dimensions`, `display.orientation` (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `screen.capture.status`, `keyboard.status`, `input.status` (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `camera.status`, `camera.permission`, `camera.list` (`IMPLEMENTED_DEVICE_DEPENDENT`)

---

## 11. Network, Location, Power & Unified Snapshot (7 Commands)

- `network.status`, `location.status`, `location.providers` (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `battery.status`, `power.status`, `background.policy`, `device.snapshot` (`IMPLEMENTED_DEVICE_DEPENDENT`)

---

## 12. Hardware Sensors (10 Commands)

- `sensor.list`, `sensor.status`, `sensor.info`, `sensor.test`, `sensor.sample`, `sensor.discovery` (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `sensor.accelerometer.sample`, `sensor.gyroscope.sample`, `sensor.proximity.sample`, `sensor.light.sample` (`IMPLEMENTED_DEVICE_DEPENDENT`)

---

## 13. System Controls & Speech (12 Commands)

- `brightness.status`, `brightness.set` (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `ringer.status`, `ringer.normal`, `ringer.vibrate`, `ringer.silent` (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `stt.status`, `stt.listen`, `stt.cancel` (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `tts.status`, `tts.speak`, `tts.stop` (`IMPLEMENTED_DEVICE_DEPENDENT`)

---

## 14. Target Resolution & Real UI Actions (12 Commands)

- `target.resolve`, `target.find`, `target.inspect`, `target.candidates` (`IMPLEMENTED`)
- `action.click` (alias `click`) (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `action.long_click` (alias `long click`) (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `action.input` (alias `text input`) (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `action.scroll` (alias `scroll forward` / `scroll backward`) (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `action.back` (alias `back`) (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `action.recents` (alias `recents`) (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `action.home` (alias `home`) (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `action.status` (`IMPLEMENTED_DIAGNOSTIC`)

---

## 15. Discovery & Namespaced Observation (8 Commands)

- `help` (`IMPLEMENTED_DIAGNOSTIC`)
- `commands` (`IMPLEMENTED_DIAGNOSTIC`)
- `observe.start`, `observe.stop`, `observe.current`, `observe.nodes` (`IMPLEMENTED_DEVICE_DEPENDENT`)
- `system.status`, `ui.state` (`IMPLEMENTED_DIAGNOSTIC`)

---

## 16. Test Harness Commands (8 Commands)

- `test.launch`, `test.observe`, `test.click`, `test.long_click`, `test.text_input`, `test.scroll`, `test.back`, `test.run` (`TEST_ONLY`)
