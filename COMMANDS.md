# LocalAgent Production Command Reference

This document is generated directly from `CommandRegistry` (the single source of truth) and covers all **53 registered production commands**.

---

## Command Coverage Overview

- **Total Registered Commands:** 53
- **Implemented Commands:** 53
- **Test Coverage:** 100% (82 test cases in `FoundationTestRegistry`)

---

## Category 1: Safety & Execution

### 1. `safety.status`
- **Name:** Safety Status
- **Category:** SAFETY
- **Syntax:** `safety status`
- **Example:** `safety status`
- **Purpose:** Queries execution controller lock state and current safety state machine status.
- **Handler:** `ExecutionController`
- **Requirements:** None
- **Test IDs:** `1.1.01`, `1.1.02`, `1.1.03`, `1.1.11`, `1.1.12`

### 2. `safety.cancel`
- **Name:** Safety Cancel
- **Category:** SAFETY
- **Syntax:** `safety cancel`
- **Example:** `safety cancel`
- **Purpose:** Requests immediate cancellation of active execution coroutine jobs.
- **Handler:** `ExecutionController`
- **Requirements:** None
- **Test IDs:** `1.1.04`, `1.1.05`, `1.1.06`

### 3. `safety.panic`
- **Name:** Panic Stop
- **Category:** SAFETY
- **Syntax:** `panic`
- **Example:** `panic`
- **Purpose:** Triggers emergency stop panic flow, releases execution lock, and issues `GLOBAL_ACTION_HOME`.
- **Handler:** `LocalAgentAccessibilityService`
- **Requirements:** Accessibility Service, Physical Observation
- **Test IDs:** `1.1.07`, `1.1.08`, `1.1.09`, `1.1.10`, `VOLUME-PHYSICAL-001`

---

## Category 2: Calculator

### 4. `calculator.calculate`
- **Name:** Calculate Math Expression
- **Category:** HEADLESS_CORE
- **Syntax:** `calculate <expression>`
- **Example:** `calculate (2 + 3) * 4`
- **Purpose:** Evaluates arithmetic math expressions supporting +, -, *, /, ^, %, parentheses, and decimals.
- **Handler:** `CalculatorSkill`
- **Requirements:** None
- **Test IDs:** `2.1.01`, `2.1.02`, `2.1.03`, `2.1.04`, `2.1.05`, `2.1.06`, `2.1.07` (division by zero), `2.1.08` (malformed)

---

## Category 3: Notes

### 5. `notes.append`
- **Name:** Append Note
- **Category:** HEADLESS_CORE
- **Syntax:** `note down <text>`
- **Example:** `note down buy milk`
- **Purpose:** Appends text entry with timestamp to persistent local note file on storage.
- **Handler:** `NotesSkill`
- **Requirements:** `WRITE_EXTERNAL_STORAGE` permission
- **Test IDs:** `2.1.09`, `2.1.10`, `2.1.11`

---

## Category 4: Timer & Alarm

### 6. `timer.create`
- **Name:** Set Timer
- **Category:** HEADLESS_CORE
- **Syntax:** `timer <seconds>`
- **Example:** `timer 60`
- **Purpose:** Launches system Clock timer intent for specified seconds.
- **Handler:** `IntentSkills`
- **Requirements:** Launches application intent
- **Test IDs:** `2.1.12`

### 7. `alarm.create`
- **Name:** Set Alarm
- **Category:** HEADLESS_CORE
- **Syntax:** `alarm <time>`
- **Example:** `alarm 07:30`
- **Purpose:** Launches system Clock alarm intent for HH:MM time.
- **Handler:** `IntentSkills`
- **Requirements:** Launches application intent
- **Test IDs:** `2.1.13`

---

## Category 5: Web Search

### 8. `web.search`
- **Name:** Web Search
- **Category:** HEADLESS_CORE
- **Syntax:** `web search <query>`
- **Example:** `web search localagent`
- **Purpose:** Dispatches `ACTION_WEB_SEARCH` intent for query string.
- **Handler:** `IntentSkills`
- **Requirements:** Launches application intent
- **Test IDs:** `2.1.14`

---

## Category 6: Application Launching & Discovery

### 9. `app.launch`
- **Name:** Launch Application
- **Category:** APPLICATION
- **Syntax:** `open <app_name>`
- **Example:** `open settings`
- **Purpose:** Resolves installed application query (exact package -> exact label -> substring match) and launches application.
- **Handler:** `AppLauncherImpl`
- **Requirements:** Physical Observation
- **Test IDs:** `2.2.01`, `2.2.02`, `2.2.03`, `2.2.04`

### 10. `app.current`
- **Name:** Current Foreground App
- **Category:** APPLICATION
- **Syntax:** `app current`
- **Example:** `app current`
- **Purpose:** Queries active foreground application package name using `UsageStatsManager`.
- **Handler:** `UsageStatsController`
- **Requirements:** `PACKAGE_USAGE_STATS` special access
- **Test IDs:** `2.5.APP.001`

### 11. `app.list`
- **Name:** List Installed Apps
- **Category:** APPLICATION
- **Syntax:** `app list`
- **Example:** `app list`
- **Purpose:** Lists all launchable installed application names and package identifiers.
- **Handler:** `AppDiscoveryController`
- **Requirements:** None
- **Test IDs:** `2.5.APP.002`

### 12. `app.find`
- **Name:** Find Application
- **Category:** APPLICATION
- **Syntax:** `app find <query>`
- **Example:** `app find settings`
- **Purpose:** Searches installed applications matching query string.
- **Handler:** `AppDiscoveryController`
- **Requirements:** None
- **Test IDs:** `2.5.APP.003`, `2.5.APP.004` (negative)

### 13. `app.info`
- **Name:** Application Package Info
- **Category:** APPLICATION
- **Syntax:** `app info <package>`
- **Example:** `app info com.android.settings`
- **Purpose:** Queries version name, version code, and target SDK for specified package name.
- **Handler:** `AppDiscoveryController`
- **Requirements:** None
- **Test IDs:** `2.5.APP.005`, `2.5.APP.006` (negative)

---

## Category 7: Flashlight

### 14. `flashlight.status`
- **Name:** Flashlight Status
- **Category:** DEVICE
- **Syntax:** `flashlight status`
- **Example:** `flashlight status`
- **Purpose:** Queries camera torch hardware availability and multi-torch mapping.
- **Handler:** `FlashlightController`
- **Requirements:** Flashlight capability
- **Test IDs:** `2.3.01`

### 15. `flashlight.on` / `flashlight.back`
- **Name:** Flashlight ON / Back Flashlight
- **Category:** DEVICE
- **Syntax:** `flashlight on` | `flashlight back`
- **Example:** `flashlight on`
- **Purpose:** Turns default/back camera torch light ON.
- **Handler:** `FlashlightController`
- **Requirements:** `CAMERA` permission, Flashlight capability, Physical Observation
- **Test IDs:** `2.3.02`, `2.3.04`

### 16. `flashlight.front`
- **Name:** Front Flashlight ON
- **Category:** DEVICE
- **Syntax:** `flashlight front`
- **Example:** `flashlight front`
- **Purpose:** Turns front camera torch light ON if present.
- **Handler:** `FlashlightController`
- **Requirements:** `CAMERA` permission, Flashlight capability, Physical Observation
- **Test IDs:** `2.3.03`

### 17. `flashlight.both`
- **Name:** Both Flashlights ON
- **Category:** DEVICE
- **Syntax:** `flashlight both`
- **Example:** `flashlight both`
- **Purpose:** Turns both front and back camera torches ON simultaneously.
- **Handler:** `FlashlightController`
- **Requirements:** `CAMERA` permission, Flashlight capability, Physical Observation
- **Test IDs:** `2.3.05`

### 18. `flashlight.off`
- **Name:** Flashlight OFF
- **Category:** DEVICE
- **Syntax:** `flashlight off`
- **Example:** `flashlight off`
- **Purpose:** Turns all camera torch lights OFF.
- **Handler:** `FlashlightController`
- **Requirements:** `CAMERA` permission, Flashlight capability, Physical Observation
- **Test IDs:** `2.3.06_OFF`

### 19. `flashlight.target`
- **Name:** Flashlight Target Control
- **Category:** DEVICE
- **Syntax:** `flashlight <target>`
- **Example:** `flashlight front` | `flashlight back` | `flashlight both` | `flashlight off` | `flashlight status`
- **Purpose:** Sets flashlight target dynamically (back, front, both, off, status); rejects invalid targets safely.
- **Handler:** `FlashlightController`
- **Requirements:** `CAMERA` permission, Flashlight capability, Physical Observation
- **Test IDs:** `2.3.07_INV` (invalid target negative), `2.3.08_NONEXIST` (non-existent target negative)

---

## Category 8: Haptics

### 17. `haptics.status`
- **Name:** Haptics Status
- **Category:** DEVICE
- **Syntax:** `vibrate status`
- **Example:** `vibrate status`
- **Purpose:** Queries device vibrator motor availability and amplitude support.
- **Handler:** `HapticController`
- **Requirements:** Vibration capability
- **Test IDs:** `2.3.10`

### 18. `haptics.vibrate`
- **Name:** Trigger Vibration
- **Category:** DEVICE
- **Syntax:** `vibrate <duration_ms>`
- **Example:** `vibrate 200`
- **Purpose:** Triggers vibration motor for duration within 1..2000ms bounds.
- **Handler:** `HapticController`
- **Requirements:** Vibration capability, Physical Observation
- **Test IDs:** `2.3.06`, `2.3.07`, `2.3.08`, `2.3.09` (invalid duration negative)

---

## Category 9: Volume Streams (Music, Ring, Alarm, Notification)

For each audio stream (`music`, `ring`, `alarm`, `notification`), 5 commands are provided:
- `volume.<stream>.status`: Queries status, current index, max index, and percentage.
- `volume.<stream>.current`: Queries current volume index integer.
- `volume.<stream>.maximum`: Queries maximum volume index integer.
- `volume.<stream>.percentage`: Queries current volume percentage (0-100%).
- `volume.<stream>.set`: Sets volume percentage (0-100%) with read-after-write verification.

**Test IDs:** `2.3.VOL.11.1` through `2.3.VOL.14.8` (Positive + Negative bounds checking)

---

## Category 10: Connectivity (Wi-Fi & Bluetooth)

### 19. `wifi.status`
- **Name:** Wi-Fi Status
- **Category:** CONNECTIVITY
- **Syntax:** `wifi status`
- **Purpose:** Queries Wi-Fi adapter enabled/disabled state.
- **Test IDs:** `2.3.30`

### 20. `wifi.on` / `wifi.off`
- **Name:** Wi-Fi Toggle Requests
- **Category:** CONNECTIVITY
- **Syntax:** `wifi on` / `wifi off`
- **Purpose:** Requests Wi-Fi enablement or disablement.
- **Test IDs:** `2.3.31`, `2.3.32`

### 21. `bluetooth.status`
- **Name:** Bluetooth Status
- **Category:** CONNECTIVITY
- **Syntax:** `bluetooth status`
- **Purpose:** Queries Bluetooth adapter state and hardware availability.
- **Test IDs:** `2.3.33`, `2.3.36`

### 22. `bluetooth.on` / `bluetooth.off`
- **Name:** Bluetooth Toggle Requests
- **Category:** CONNECTIVITY
- **Syntax:** `bluetooth on` / `bluetooth off`
- **Purpose:** Requests Bluetooth toggle; returns `BLOCKED / UNSUPPORTED_DIRECT_CONTROL` on API 27+ and launches Settings.
- **Test IDs:** `2.3.34`, `2.3.35`

---

## Category 11: Clipboard

### 23. `clipboard.status`
- **Syntax:** `clipboard status` | **Purpose:** Queries system clipboard service state. | **Test ID:** `2.5.CLIP.001`

### 24. `clipboard.read`
- **Syntax:** `clipboard read` | **Purpose:** Reads text from clipboard. | **Test ID:** `2.5.CLIP.003`

### 25. `clipboard.write`
- **Syntax:** `clipboard write <text>` | **Purpose:** Writes text to clipboard. | **Test ID:** `2.5.CLIP.002`

### 26. `clipboard.clear`
- **Syntax:** `clipboard clear` | **Purpose:** Clears system clipboard contents. | **Test ID:** `2.5.CLIP.004`

---

## Category 12: Notification Listener

### 27. `notification.status`
- **Syntax:** `notification status` | **Purpose:** Queries `NotificationListenerService` access and connection status. | **Test ID:** `2.5.NOTIF.001`

### 28. `notification.latest`
- **Syntax:** `notification latest` | **Purpose:** Reads latest received notification snapshot. | **Test ID:** `2.5.NOTIF.002`

---

## Category 13: Display & Input

### 29. `display.status`
- **Syntax:** `display status` | **Purpose:** Queries screen metrics, density, and orientation. | **Test ID:** `2.5.DISP.001`

### 30. `display.dimensions`
- **Syntax:** `display dimensions` | **Purpose:** Queries screen pixel width and height. | **Test ID:** `2.5.DISP.002`

### 31. `display.orientation`
- **Syntax:** `display orientation` | **Purpose:** Queries screen orientation state. | **Test ID:** `2.5.DISP.003`

### 32. `screen.capture.status`
- **Syntax:** `screen capture status` | **Purpose:** Queries screen capture capability state. | **Test ID:** `2.5.DISP.004`

### 33. `keyboard.status`
- **Syntax:** `keyboard status` | **Purpose:** Queries soft keyboard visibility state. | **Test ID:** `2.5.INP.001`

### 34. `input.status`
- **Syntax:** `input status` | **Purpose:** Queries IME input method status. | **Test ID:** `2.5.INP.002`

---

## Category 14: Camera

### 35. `camera.status`
- **Syntax:** `camera status` | **Purpose:** Queries camera hardware availability and count. | **Test ID:** `2.5.CAM.001`

### 36. `camera.permission`
- **Syntax:** `camera permission` | **Purpose:** Queries `CAMERA` runtime permission status. | **Test ID:** `2.5.CAM.002`

### 37. `camera.list`
- **Syntax:** `camera list` | **Purpose:** Lists installed camera device IDs. | **Test ID:** `2.5.CAM.003`

---

## Category 15: Network, Location, Power & Device Snapshot

### 38. `network.status`
- **Syntax:** `network status` | **Purpose:** Queries active network connection status. | **Test ID:** `2.5.NET.001`

### 39. `location.status`
- **Syntax:** `location status` | **Purpose:** Queries GPS and Network provider enablement states. | **Test ID:** `2.3.44`

### 40. `location.providers`
- **Syntax:** `location providers` | **Purpose:** Queries location provider details. | **Test ID:** `2.5.LOC.001`

### 41. `battery.status`
- **Syntax:** `battery status` | **Purpose:** Queries battery level % and charging state. | **Test ID:** `2.3.45`

### 42. `power.status`
- **Syntax:** `power status` | **Purpose:** Queries screen interactivity power state. | **Test ID:** `2.5.PWR.001`

### 43. `background.policy`
- **Syntax:** `background policy` | **Purpose:** Queries background execution policy. | **Test ID:** `2.5.BG.001`

### 44. `device.snapshot`
- **Syntax:** `device snapshot` | **Purpose:** Aggregates unified device state snapshot. | **Test ID:** `2.5.SNAP.001`

---

## Category 16: Sensors

### 45. `sensor.list`
- **Syntax:** `sensor list` | **Purpose:** Enumerates hardware sensors via SensorManager. | **Test ID:** `2.3.46`

### 46. `sensor.accelerometer.sample`
- **Syntax:** `sensor accelerometer` | **Purpose:** Samples 3-axis accelerometer values. | **Test ID:** `2.3.47`

### 47. `sensor.gyroscope.sample`
- **Syntax:** `sensor gyroscope` | **Purpose:** Samples 3-axis gyroscope values. | **Test ID:** `2.3.48`

### 48. `sensor.proximity.sample`
- **Syntax:** `sensor proximity` | **Purpose:** Samples proximity sensor distance (cm) with range metadata. | **Test ID:** `2.3.49`

### 49. `sensor.light.sample`
- **Syntax:** `sensor light` | **Purpose:** Samples ambient light illuminance (lux). | **Test ID:** `2.3.50`

---

## Category 17: System Controls

### 50. `brightness.status`
- **Syntax:** `brightness status` | **Purpose:** Reads current screen brightness level. | **Test ID:** `2.3.37`, `2.3.40`

### 51. `brightness.set`
- **Syntax:** `brightness <percentage>` | **Purpose:** Sets brightness level (0-100%) requiring `WRITE_SETTINGS`. | **Test ID:** `2.3.38`, `2.3.39`

### 52. `ringer.status`
- **Syntax:** `ringer status` | **Purpose:** Queries current ringer mode. | **Test ID:** `2.3.41`

### 53. `ringer.normal` / `ringer.vibrate` / `ringer.silent`
- **Syntax:** `ringer normal` / `ringer vibrate` / `ringer silent`
- **Purpose:** Sets ringer mode; requires Notification Policy Access (Do Not Disturb Access).
- **Test IDs:** `2.3.42`, `2.3.43`, `2.5.RING.001`

---

## Category 18: Speech (STT & TTS)

- `stt.status`: Queries SpeechRecognizer availability (`2.4.01`).
- `stt.listen`: Starts STT speech recognition listener requiring `RECORD_AUDIO` (`2.4.02`).
- `stt.cancel`: Cancels active speech recognition session (`2.5.STT.001`).
- `tts.status`: Queries TextToSpeech engine status (`2.4.03`).
- `tts.speak`: Speaks text using native TTS (`2.4.04`).
- `tts.stop`: Stops active speech output (`2.5.TTS.001`).

---

## Category 19: Permissions & Diagnostics

- `permissions.status`: Queries runtime permissions & special access status (`2.5.DIAG.001`).
- `capabilities.status`: Queries hardware capability states (`2.5.DIAG.002`).
- `accessibility.status`: Queries accessibility service connection (`2.5.DIAG.003`).
- `diagnostics.status`: Generates complete device diagnostics report (`2.5.DIAG.004`).
- `diagnostics.readiness`: Evaluates 9 deterministic foundation readiness gates (`2.5.DIAG.005`).
