# LocalAgent Settings Navigation Fallbacks & Android API-27 Restrictions

## Settings Action Registry (`SettingsActionRegistry`)
Resolves and launches safe Settings intents:
- `settings.accessibility`: `Settings.ACTION_ACCESSIBILITY_SETTINGS`
- `settings.display`: `Settings.ACTION_DISPLAY_SETTINGS`
- `settings.sound`: `Settings.ACTION_SOUND_SETTINGS`
- `settings.wifi`: `Settings.ACTION_WIFI_SETTINGS`
- `settings.bluetooth`: `Settings.ACTION_BLUETOOTH_SETTINGS`
- `settings.location`: `Settings.ACTION_LOCATION_SOURCE_SETTINGS`
- `settings.battery`: `Settings.ACTION_BATTERY_SAVER_SETTINGS`
- `settings.apps`: `Settings.ACTION_APPLICATION_SETTINGS`
- `settings.storage`: `Settings.ACTION_INTERNAL_STORAGE_SETTINGS`
- `settings.security`: `Settings.ACTION_SECURITY_SETTINGS`
- `settings.date_time`: `Settings.ACTION_DATE_SETTINGS`
- `settings.input`: `Settings.ACTION_INPUT_METHOD_SETTINGS`
- `settings.developer`: `Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS`

## API-27 Platform Restrictions
- Direct Wi-Fi and Bluetooth toggles are restricted on newer Android releases; settings fallbacks are launched.
- Privileged operations (Device Owner, Profile Owner, System App) return explicit classification error codes (`DEVICE_OWNER_REQUIRED`, `SYSTEM_PRIVILEGED_REQUIRED`) rather than resorting to shell/root/reflection hacks.
