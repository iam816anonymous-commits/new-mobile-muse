# LocalAgent Foundation Gaps & OS Constraints

## Overview
This document logs verified Android OS limits and hardware capability constraints encountered on API 27 / Android 8.1 and target devices.

## Identified Constraints & Fallback Solutions

1. **Wi-Fi & Bluetooth Direct Toggle Restrictions**:
   - *Constraint*: Android 10+ (API 29+) restricts apps from directly enabling/disabling Wi-Fi and Bluetooth programmatically via `WifiManager.setWifiEnabled()` or `BluetoothAdapter.enable()`.
   - *Solution*: LocalAgent queries status accurately (`WIFI` and `BLUETOOTH` capabilities reported as `AVAILABLE` if hardware present) and launches system settings intents (`Settings.ACTION_WIFI_SETTINGS`, `Settings.ACTION_BLUETOOTH_SETTINGS`) when direct toggles are restricted.

2. **Special Settings Access (`WRITE_SETTINGS` & Notification Policy Access)**:
   - *Constraint*: Android does not allow apps to request or grant special settings access directly via runtime permission dialogs.
   - *Solution*: Dedicated Permissions UI detects status (`Settings.System.canWrite()`, `isNotificationPolicyAccessGranted()`) and provides direct guided navigation buttons (`Settings.ACTION_MANAGE_WRITE_SETTINGS`, `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`).

3. **Screenshot Restrictions & Evidence Fallback**:
   - *Constraint*: Certain protected views or background Activity states prevent PixelCopy API screenshot capture.
   - *Solution*: `EvidenceManager` uses decorView drawing fallback and records `EVIDENCE_UNAVAILABLE` when capture is disallowed, preventing app crashes or fake screenshots.

4. **Speech Recognition On-Device Availability**:
   - *Constraint*: On some low-end or customized ROM devices, Android's built-in `SpeechRecognizer` service may be missing or uninstalled.
   - *Solution*: `SpeechToTextEngine.isAvailable()` detects availability and marks capability as `UNSUPPORTED` without crashing.
