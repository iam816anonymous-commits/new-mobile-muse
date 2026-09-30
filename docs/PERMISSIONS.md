# LocalAgent Permission & Special Access Model

## Centralized Subsystem
The permission system is governed by `com.agent.android.permissions`:
- `PermissionDefinition`
- `PermissionCategory`
- `PermissionStatus`
- `PermissionRegistry`
- `PermissionManager`

## Permission Categories
1. **RUNTIME**: Runtime dangerous permissions requested dynamically (`RECORD_AUDIO`, `CAMERA`, `LOCATION`, `STORAGE`, `READ_PHONE_STATE`).
2. **SYSTEM_SETTING**: Write Settings special access (`WRITE_SETTINGS`).
3. **SPECIAL_ACCESS**: Notification Policy Access (DND).
4. **NOTIFICATION_ACCESS**: Notification Listener Service (`BIND_NOTIFICATION_LISTENER_SERVICE`).
5. **USAGE_ACCESS**: Usage Access (`PACKAGE_USAGE_STATS`).
6. **ACCESSIBILITY**: Accessibility Service (`BIND_ACCESSIBILITY_SERVICE`).
7. **PRIVILEGED_ONLY**: System/Device-owner privileges (classified as non-grantable for ordinary applications).

## Settings Fallbacks
When direct programmatic permission grants are prohibited by Android 8.1, `PermissionManager.openSettings(perm)` launches the targeted Android System Settings screen (`ACTION_MANAGE_WRITE_SETTINGS`, `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`, `ACTION_NOTIFICATION_LISTENER_SETTINGS`, `ACTION_USAGE_ACCESS_SETTINGS`, `ACTION_ACCESSIBILITY_SETTINGS`).
