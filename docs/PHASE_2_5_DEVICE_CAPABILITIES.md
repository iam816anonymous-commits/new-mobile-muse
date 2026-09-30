# LocalAgent Phase 2.5 Device Capabilities Audit

## Capability Classification Matrix

| Capability ID | Category | Classification | Observe | Control | Permissions / Special Access | Settings Fallback |
|---|---|---|---|---|---|---|
| `FLASHLIGHT` | DEVICE | NORMAL_APP_SUPPORTED | Yes | Yes | `CAMERA` | Yes |
| `VIBRATION` | DEVICE | NORMAL_APP_SUPPORTED | Yes | Yes | None | N/A |
| `VOLUME` | DEVICE | NORMAL_APP_SUPPORTED | Yes | Yes | None | `Settings.ACTION_SOUND_SETTINGS` |
| `WIFI` | CONNECTIVITY | NORMAL_APP_SUPPORTED | Yes | Yes | `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE` | `Settings.ACTION_WIFI_SETTINGS` |
| `BLUETOOTH` | CONNECTIVITY | NORMAL_APP_SUPPORTED | Yes | Restricted on Android 10+ | `BLUETOOTH`, `BLUETOOTH_ADMIN` | `Settings.ACTION_BLUETOOTH_SETTINGS` |
| `CLIPBOARD` | DEVICE | NORMAL_APP_SUPPORTED | Yes | Yes | None | N/A |
| `SENSORS` | OBSERVATION | NORMAL_APP_SUPPORTED | Yes | N/A | None | N/A |
| `BRIGHTNESS` | SYSTEM_CONTROLS | SPECIAL_ACCESS_REQUIRED | Yes | Yes | `WRITE_SETTINGS` | `Settings.ACTION_MANAGE_WRITE_SETTINGS` |
| `RINGER` | SYSTEM_CONTROLS | SPECIAL_ACCESS_REQUIRED | Yes | Yes | Notification Policy Access | `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` |
| `NOTIFICATIONS` | OBSERVATION | SPECIAL_ACCESS_REQUIRED | Yes | No | Notification Listener Access | `enabled_notification_listeners` |
| `USAGE_STATS` | APPLICATION | SPECIAL_ACCESS_REQUIRED | Yes | No | `USAGE_STATS_ACCESS` | `Usage Access` Settings |
| `ACCESSIBILITY` | DIAGNOSTICS | ACCESSIBILITY_REQUIRED | Yes | Abort only | Accessibility Service | `Settings.ACTION_ACCESSIBILITY_SETTINGS` |
| `DEVICE_OWNER` | DIAGNOSTICS | DEVICE_OWNER_REQUIRED | Yes | No | Provisioning required | N/A |
