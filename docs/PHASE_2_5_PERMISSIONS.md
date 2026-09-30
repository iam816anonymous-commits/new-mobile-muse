# LocalAgent Phase 2.5 Permissions & Special Access

## Runtime Permissions
- `RECORD_AUDIO`: Speech-to-Text (`stt.listen`).
- `CAMERA`: Flashlight Torch & Camera Info.
- `WRITE_EXTERNAL_STORAGE`: Notes persistent storage.
- `READ_EXTERNAL_STORAGE`: Media / Notes reading.

## Special Accesses
1. `WRITE_SETTINGS`: Screen brightness and screen timeout controls (`Settings.System.canWrite`).
2. `Notification Policy Access`: Silent Do Not Disturb ringer mode toggle (`NotificationManager.isNotificationPolicyAccessGranted`).
3. `Notification Listener Access`: Inspection of posted system notifications (`NotificationListenerService`).
4. `Usage Access`: Foreground application package detection (`UsageStatsManager`).
5. `Accessibility Service`: Panic gesture observation and navigation (`LocalAgentAccessibilityService`).
