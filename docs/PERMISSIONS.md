# LocalAgent Permissions & Special Access

## Overview
LocalAgent manages permissions through a structured, transparent model categorized by runtime perms, special access, device capabilities, and system services.

## Permission Categories

### 1. Runtime Permissions
- `android.permission.WRITE_EXTERNAL_STORAGE`: Required for Notes skill persistent file writing on Android 8.1 / API 27.
- `android.permission.CAMERA`: Required for Flashlight (Camera torch) hardware control.
- `android.permission.RECORD_AUDIO`: Required for Android built-in SpeechRecognizer (Speech-to-Text).

### 2. Special Access
- `Accessibility Service` (`com.agent.android.service.LocalAgentAccessibilityService`): Required for hardware Volume-Up panic gesture observation and return-to-home navigation. Guided via `Settings.ACTION_ACCESSIBILITY_SETTINGS`.
- `Write System Settings` (`android.permission.WRITE_SETTINGS`): Required for screen brightness and screen timeout controls. Checked via `Settings.System.canWrite(context)`. Guided via `Settings.ACTION_MANAGE_WRITE_SETTINGS`.
- `Notification Policy Access`: Required for Silent / Do Not Disturb ringer mode toggle. Guided via `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`.

### 3. Device Capabilities
- `FLASHLIGHT`: Camera torch feature check.
- `VIBRATION`: Vibrator service check.
- `WIFI` & `BLUETOOTH`: Radio status and settings intents. Direct toggling restricted on Android 10+.
- `SENSORS`: Accelerometer, Gyroscope, Proximity, Light.

### 4. System Services
- `SpeechToTextEngine`: Platform `SpeechRecognizer` API.
- `TextToSpeechEngine`: Platform `TextToSpeech` API.

### 5. Future Capabilities (Phase 3+)
- `SYSTEM_ALERT_WINDOW` (Overlay): Not requested in Phase 2.4.
- `RECEIVE_BOOT_COMPLETED`: Not requested in Phase 2.4.
- `ACCESS_FINE_LOCATION`: Optional for future location precision.
