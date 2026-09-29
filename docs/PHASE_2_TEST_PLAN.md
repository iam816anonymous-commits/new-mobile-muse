# Phase 2 Test Plan

## Unit Tests (`Phase2HeadlessUnitTest.kt` & `Phase2DeviceUnitTest.kt`)

1. **`testCalculatorExpressionPrecedenceAndParentheses`**: Verifies arithmetic operator precedence (`2 + 3 * 4 = 14`), parentheses (`(2 + 3) * 4 = 20`), and decimals (`10.5 / 2 = 5.25`).
2. **`testCalculatorErrorHandling`**: Verifies division by zero returns `DIVISION_BY_ZERO` and malformed input returns `INVALID_EXPRESSION`.
3. **`testNotesSkillEmptyContent`**: Verifies blank notes return `EMPTY_NOTE` error status.
4. **`testGoalDispatcherRoutingAndSafetyWrapping`**: Verifies deterministic command parsing and execution lock integration.
5. **`testPhase2HeadlessTestHarnessSuite`**: Executes `Phase2HeadlessTestHarness` suite.
6. **`testFlashlightNoContextSafeguard`**: Verifies null context safeguard returns `NO_CONTEXT`.
7. **`testHapticNoContextSafeguard`**: Verifies null context safeguard returns `NO_CONTEXT`.
8. **`testVolumeNoContextSafeguard`**: Verifies null context safeguard returns `UNAVAILABLE`.
9. **`testConnectivityNoContextSafeguard`**: Verifies Wi-Fi/Bluetooth null context safeguards.
10. **`testHardwareObservationNoContextSafeguard`**: Verifies battery null context safeguard.
11. **`testPhase2DeviceTestHarnessSuite`**: Executes `Phase2DeviceTestHarness` suite.

## Physical Device Verification Procedures

1. **Calculator**: Execute `calculate 25 * 2` -> Expect `50`.
2. **Notes**: Execute `note down Phase 2 test` -> Verify entry in `Download/LocalAgent/notes.txt`.
3. **Timer**: Execute `timer 1 minute` -> Verify Clock timer screen opens.
4. **Alarm**: Execute `set alarm for 07:30` -> Verify Clock alarm screen opens.
5. **Web Search**: Execute `search Google for Android` -> Verify Browser opens.
6. **Flashlight**: Toggle Flashlight ON/OFF.
7. **Haptics**: Trigger Vibrate 300ms.
8. **Battery**: Read Battery percentage and charging status.
