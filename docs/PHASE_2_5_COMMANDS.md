# LocalAgent Phase 2.5 Commands Inventory

All 53 production commands are registered in `CommandRegistry`:
- **SAFETY**: `safety.status`, `safety.cancel`, `safety.panic`
- **HEADLESS_CORE**: `calculator.calculate`, `notes.append`, `timer.create`, `alarm.create`, `web.search`
- **APPLICATION**: `app.launch`, `app.current`, `app.list`, `app.find`, `app.info`
- **DEVICE**: `flashlight.status`, `flashlight.on`, `flashlight.off`, `haptics.status`, `haptics.vibrate`, `volume.*` (alarm, ring, notification, music status/current/max/percentage/set), `clipboard.status`, `clipboard.read`, `clipboard.write`, `clipboard.clear`, `display.status`, `display.dimensions`, `display.orientation`, `screen.capture.status`, `camera.status`, `camera.permission`, `camera.list`
- **CONNECTIVITY**: `wifi.status`, `wifi.on`, `wifi.off`, `bluetooth.status`, `bluetooth.on`, `bluetooth.off`
- **OBSERVATION**: `battery.status`, `sensor.list`, `sensor.*.sample`, `notification.status`, `notification.latest`, `keyboard.status`, `input.status`, `network.status`, `location.providers`, `power.status`
- **SYSTEM_CONTROLS**: `brightness.status`, `brightness.set`, `ringer.status`, `ringer.normal`, `ringer.vibrate`, `ringer.silent`, `location.status`
- **SPEECH**: `stt.status`, `stt.listen`, `stt.cancel`, `tts.status`, `tts.speak`, `tts.stop`
- **DIAGNOSTICS**: `permissions.status`, `capabilities.status`, `accessibility.status`, `diagnostics.status`, `diagnostics.readiness`, `background.policy`, `device.snapshot`, `settings.*`
