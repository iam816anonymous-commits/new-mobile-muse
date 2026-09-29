# Phase 2.1 Failure Analysis & Root Cause Report

**Target Device**: Tecno Camon i (Android 8.1 / API 27, MediaTek Helio P23, 4 GB RAM)

## Physical Failure Findings & Hardening Decisions

### 1. User Notes (`note down ...`)
* **Observed Failure**: Failed with permission denied / I/O error.
* **Root Cause**: Writing to `Environment.DIRECTORY_DOWNLOADS` on Android 8.1 requires runtime `WRITE_EXTERNAL_STORAGE` permission.
* **Fix / Hardening**: Added runtime permission check in `NotesSkill.kt`. If permission is not granted, returns `PERMISSION_REQUIRED` without crashing. Added First-Run Permission Setup UI section in `MainActivity`.

### 2. Vibration (`vibrate -500`)
* **Observed Failure**: Negative and excessive durations passed directly to system service.
* **Root Cause**: Lack of input validation in `HapticController.kt`.
* **Fix / Hardening**: Added strict input validation bounds (`1ms`..`2000ms`). Out-of-bounds inputs return `FAILED` with `errorCode = INVALID_ARGUMENT`.

### 3. Volume Control
* **Observed Failure**: Absolute index miscalculation across hardware audio streams.
* **Root Cause**: Device audio streams have varying max steps (e.g. 15 steps). Passing percentages directly caused index mismatch.
* **Fix / Hardening**: Implemented percentage-to-index mapping (`(percent / 100.0) * maxVolume`). Added read-after-write verification to confirm volume index match.

### 4. Bluetooth Toggle
* **Observed Failure**: `bluetooth status` worked, but direct ON/OFF toggling failed on API 27 without system settings path.
* **Root Cause**: Android OS restrictions prevent non-system apps from toggling Bluetooth directly on specific target configurations.
* **Fix / Hardening**: Bluetooth toggle returns `UNSUPPORTED` with `errorCode = UNSUPPORTED_DIRECT_CONTROL` and logs guidance to system settings path rather than falsely reporting success.

### 5. Timer & Alarm Intents
* **Observed Failure**: Returned `Timer activity unavailable` / `Activity not found`.
* **Root Cause**: Device stock ROM lacks standard `AlarmClock.ACTION_SET_TIMER` and `ACTION_SET_ALARM` activity handlers.
* **Fix / Hardening**: Added explicit `PackageManager.resolveActivity()` checks before launching. If no activity is resolved, returns `UNSUPPORTED` with `errorCode = ACTIVITY_UNAVAILABLE`.

### 6. Sensor Sampling Timeouts
* **Observed Failure**: On-demand sensor sampling timed out for Accelerometer, Gyroscope, Proximity, and Light.
* **Root Cause**: Helio P23 OEM sensor event dispatch delays or absent physical sensor hardware.
* **Fix / Hardening**: Created `HardwareObservationControllers.runDeviceDiagnostics()` to report sensor presence, vendor, version, power, resolution, and max range. Added 1000ms sampling timeout with guaranteed unregistration in `finally` blocks.
