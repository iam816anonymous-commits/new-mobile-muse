# LocalAgent Android 8.1 (API 27) Permission & Special Access Audit

## 1. Permission Matrix
| Access ID | Android Permission / Action | Category | Manifest Required? | Runtime Request? | Settings Required? | User Grantable? | API 27 Available? | LocalAgent Relevant? | Related Commands | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `perm_record_audio` | `android.permission.RECORD_AUDIO` | RUNTIME | Yes | Yes | No | Yes | Yes | Yes | `stt.listen` | DANGEROUS |
| `perm_camera` | `android.permission.CAMERA` | RUNTIME | Yes | Yes | No | Yes | Yes | Yes | `flashlight.on`, `flashlight.off`, `camera.*` | DANGEROUS |
| `perm_fine_location` | `android.permission.ACCESS_FINE_LOCATION` | RUNTIME | Yes | Yes | No | Yes | Yes | Yes | `location.status`, `location.providers` | DANGEROUS |
| `perm_coarse_location` | `android.permission.ACCESS_COARSE_LOCATION` | RUNTIME | Yes | Yes | No | Yes | Yes | Yes | `location.status`, `location.providers` | DANGEROUS |
| `perm_read_storage` | `android.permission.READ_EXTERNAL_STORAGE` | RUNTIME | Yes | Yes | No | Yes | Yes | Yes | `notes.append` | DANGEROUS |
| `perm_write_storage` | `android.permission.WRITE_EXTERNAL_STORAGE` | RUNTIME | Yes | Yes | No | Yes | Yes | Yes | `notes.append` | DANGEROUS |
| `perm_read_phone_state` | `android.permission.READ_PHONE_STATE` | RUNTIME | Yes | Yes | No | Yes | Yes | Yes | `device.snapshot` | DANGEROUS |
| `perm_vibrate` | `android.permission.VIBRATE` | RUNTIME | Yes | No | No | Yes (Install) | Yes | Yes | `haptics.vibrate`, `ringer.vibrate` | NORMAL |
| `write_settings_access` | `android.permission.WRITE_SETTINGS` | SYSTEM_SETTING | Yes | No | Yes | Yes (Settings) | Yes | Yes | `brightness.set` | SPECIAL_ACCESS |
| `notification_policy_access` | `android.permission.ACCESS_NOTIFICATION_POLICY` | SPECIAL_ACCESS | Yes | No | Yes | Yes (Settings) | Yes | Yes | `ringer.normal`, `ringer.vibrate`, `ringer.silent` | SPECIAL_ACCESS |
| `notification_listener_access` | `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` | NOTIFICATION_ACCESS | Yes | No | Yes | Yes (Settings) | Yes | Yes | `notification.status`, `notification.latest` | SPECIAL_ACCESS |
| `usage_stats_access` | `android.permission.PACKAGE_USAGE_STATS` | USAGE_ACCESS | Yes | No | Yes | Yes (Settings) | Yes | Yes | `app.current` | SPECIAL_ACCESS |
| `accessibility_service_required` | `android.permission.BIND_ACCESSIBILITY_SERVICE` | ACCESSIBILITY | Yes | No | Yes | Yes (Settings) | Yes | Yes | `accessibility.status`, `safety.panic` | SPECIAL_ACCESS |
| `device_admin_access` | `android.permission.BIND_DEVICE_ADMIN` | SPECIAL_ACCESS | Yes | No | Yes | Yes (Settings) | Yes | Yes | `permissions.status` | SPECIAL_ACCESS |
| `device_owner_access` | `android.permission.BIND_DEVICE_ADMIN` | PRIVILEGED_ONLY | No | No | No | No | N/A | No | N/A | PRIVILEGED_ONLY |

## 2. API 27 Platform Specifics
- **Device Administration**: `LocalAgentAdminReceiver` is declared in `AndroidManifest.xml` with `device_admin_policies.xml`. `device_admin_access` is user-grantable via Settings -> Device Administrators. Device Owner privileges remain explicitly classified as `PRIVILEGED_ONLY` (system provisioning required).
- **Direct Bluetooth Control**: Restricted by OEM/Android 8.1 OS policies; direct toggling returns `BLOCKED / UNSUPPORTED_DIRECT_CONTROL` and opens Settings screen.
- **Ringer Mode Modifications**: Requires Notification Policy Access (Do Not Disturb Special Access) on Android 7.0+ (API 24+).
- **Foreground App Querying**: Requires `PACKAGE_USAGE_STATS` special access via Settings -> Usage Access checked via `AppOpsManager`.
- **Notification Traversal**: Requires `BIND_NOTIFICATION_LISTENER_SERVICE` special access via Settings -> Notification Access checked via `enabled_notification_listeners`.
