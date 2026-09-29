# Physical Panic Button Architecture

## Panic Gesture Definition

* **Hardware Input**: Double-press of physical Volume-Up key (`KEYCODE_VOLUME_UP`, `ACTION_DOWN`) within `500ms`.
* **Accessibility Flag**: `flagRequestFilterKeyEvents` in `res/xml/accessibility_service_config.xml`.
* **Filtering**: Intercepted in `LocalAgentAccessibilityService.onKeyEvent()`.

## Emergency Stop Mechanics

Upon panic gesture detection:
1. Logs emergency stop event in `Logger`.
2. Requests cancellation with `CancellationReason.USER_PANIC`.
3. Cancels active execution `Job` and releases `ExecutionController` ownership.
4. Performs Android system `GLOBAL_ACTION_HOME` to return device to home screen.
5. Updates `SafetyState` to `PANIC`.
6. Consumes the key event (`return true`).
