# LocalAgent Foundation Gaps & OS Constraints (Phase 2.4.1)

## Overview
This document logs verified Android OS platform constraints and special access requirements.

## Verified Platform Constraints
1. **Bluetooth & Wi-Fi Direct Toggle Restrictions**:
   - *Constraint*: Android 10+ (API 29+) and certain OEM ROMs restrict apps from programmatically toggling Bluetooth and Wi-Fi adapters directly.
   - *Resolution*: Direct toggles return `BLOCKED / UNSUPPORTED_DIRECT_CONTROL` and launch system settings intents (`Settings.ACTION_BLUETOOTH_SETTINGS`, `Settings.ACTION_WIFI_SETTINGS`) as fallback. No shell/root/reflection hacks are used.

2. **Notification Policy Access (Do Not Disturb / Silent Ringer Mode)**:
   - *Constraint*: Changing ringer mode to SILENT on Android 7.0+ (API 24+) requires Notification Policy Access (`isNotificationPolicyAccessGranted`).
   - *Resolution*: `ringer.silent` returns `BLOCKED / NOTIFICATION_POLICY_ACCESS_REQUIRED` when access is not granted, guiding the user to `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`. Read-only `ringer.status` operates independently without requiring special access.

3. **Write System Settings (`WRITE_SETTINGS`)**:
   - *Constraint*: Modifying system brightness or screen timeout requires `WRITE_SETTINGS` special access.
   - *Resolution*: `brightness.set` returns `BLOCKED / WRITE_SETTINGS_REQUIRED` when access is missing, guiding the user to `Settings.ACTION_MANAGE_WRITE_SETTINGS`. Read-only `brightness.status` queries level independently.
