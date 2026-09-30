package com.agent.android.test

import com.agent.android.test.model.TestCase
import com.agent.android.test.model.TestStatus
import com.agent.android.test.model.TestType

class FoundationTestRegistry {

    private val testCases: MutableList<TestCase> = mutableListOf()

    init {
        populateDefaultTestCases()
    }

    private fun populateDefaultTestCases() {
        testCases.clear()

        // PHASE 1: SAFETY (12)
        add(TestCase("1.1.01", "safety.status", "PHASE_1", "SAFETY", "Execution State Transitions", "Verifies state machine transitions through IDLE, EXECUTING, and back to IDLE.", null, "Acquired=true, Executing=true -> Released=true, State=IDLE", TestType.SAFETY))
        add(TestCase("1.1.02", "safety.status", "PHASE_1", "SAFETY", "Invalid Execution Transitions", "Verifies invalid state transition attempts are safely handled.", null, "Invalid transition rejected safely", TestType.SAFETY))
        add(TestCase("1.1.03", "safety.status", "PHASE_1", "SAFETY", "Execution Lock Acquisition", "Verifies global lock prevents unauthenticated execution.", null, "Lock acquired and released cleanly", TestType.SAFETY))
        add(TestCase("1.1.04", "safety.cancel", "PHASE_1", "SAFETY", "Concurrent Execution Rejection", "Verifies 2nd concurrent execution is rejected.", null, "First=ACCEPTED, Second=REJECTED, Active=1", TestType.SAFETY))
        add(TestCase("1.1.05", "safety.cancel", "PHASE_1", "SAFETY", "Watchdog Timeout", "Verifies MasterWatchdog cancels hung execution after budget expiration.", null, "Cancelled=true, Reason=WATCHDOG_TIMEOUT", TestType.SAFETY))
        add(TestCase("1.1.06", "safety.cancel", "PHASE_1", "SAFETY", "Manual Cancellation", "Verifies user manual cancellation immediately halts active job.", null, "CancellationRequested=true, JobCancelled=true", TestType.SAFETY))
        add(TestCase("1.1.07", "safety.panic", "PHASE_1", "SAFETY", "Panic Logic Simulation", "Verifies panic emergency stop path resets state and transitions to PANIC.", null, "Reason=USER_PANIC, Status=PANIC, Executing=false", TestType.SAFETY))
        add(TestCase("1.1.08", "safety.panic", "PHASE_1", "SAFETY", "Volume-Up Normal Behavior", "Single Volume-Up press must pass through to Android OS normally.", "press volume up", "Volume key event returned false (unconsumed)", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("1.1.09", "safety.panic", "PHASE_1", "SAFETY", "Double Volume-Up Panic Gesture", "Double Volume-Up within 500ms must trigger emergency stop.", "double press volume up", "Panic triggered, execution cancelled", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("1.1.10", "safety.panic", "PHASE_1", "SAFETY", "Panic Return To Home", "Panic gesture must issue GLOBAL_ACTION_HOME.", "double press volume up", "Device returns to Android Home Screen", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("1.1.11", "safety.status", "PHASE_1", "SAFETY", "Execution Reset After Cancellation", "System must return to IDLE and accept new commands after cancellation.", null, "New execution accepted after cancellation", TestType.SAFETY))
        add(TestCase("1.1.12", "safety.status", "PHASE_1", "SAFETY", "Execution Reset After Timeout", "System must return to IDLE and accept new commands after timeout.", null, "New execution accepted after timeout", TestType.SAFETY))

        // HEADLESS CORE (15)
        add(TestCase("2.1.01", "calculator.calculate", "PHASE_2", "HEADLESS_CORE", "Calculate Integer", "Evaluates basic integer arithmetic.", "calculate 12 + 34", "Result: 46", TestType.AUTOMATED))
        add(TestCase("2.1.02", "calculator.calculate", "PHASE_2", "HEADLESS_CORE", "Calculate Decimal", "Evaluates decimal floating point arithmetic.", "calculate 12.5 * 4", "Result: 50.0", TestType.AUTOMATED))
        add(TestCase("2.1.03", "calculator.calculate", "PHASE_2", "HEADLESS_CORE", "Calculate Precedence", "Evaluates operator precedence rules (* before +).", "calculate 2 + 3 * 4", "Result: 14", TestType.AUTOMATED))
        add(TestCase("2.1.04", "calculator.calculate", "PHASE_2", "HEADLESS_CORE", "Calculate Parentheses", "Evaluates explicit parenthesized expressions.", "calculate (2 + 3) * 4", "Result: 20", TestType.AUTOMATED))
        add(TestCase("2.1.05", "calculator.calculate", "PHASE_2", "HEADLESS_CORE", "Calculate Exponent", "Evaluates exponentiation operator.", "calculate 2 ^ 3", "Result: 8", TestType.AUTOMATED))
        add(TestCase("2.1.06", "calculator.calculate", "PHASE_2", "HEADLESS_CORE", "Calculate Modulo", "Evaluates modulo remainder operation.", "calculate 10 % 3", "Result: 1", TestType.AUTOMATED))
        add(TestCase("2.1.07", "calculator.calculate", "PHASE_2", "HEADLESS_CORE", "Division By Zero", "Handles division by zero safely without crash.", "calculate 10 / 0", "DIVISION_BY_ZERO", TestType.NEGATIVE))
        add(TestCase("2.1.08", "calculator.calculate", "PHASE_2", "HEADLESS_CORE", "Malformed Expression", "Handles syntax error in math expression.", "calculate 2 + + 3", "MALFORMED_EXPRESSION", TestType.NEGATIVE))
        add(TestCase("2.1.09", "notes.append", "PHASE_2", "HEADLESS_CORE", "Notes Permission Check", "Verifies storage permission handling for notes.", "note down test note", "Notes updated or permission required", TestType.PERMISSION, requiredPermission = "android.permission.WRITE_EXTERNAL_STORAGE"))
        add(TestCase("2.1.10", "notes.append", "PHASE_2", "HEADLESS_CORE", "Note Down Entry", "Appends text entry to persistent note file.", "note down test entry", "Successfully appended note", TestType.AUTOMATED, requiredPermission = "android.permission.WRITE_EXTERNAL_STORAGE"))
        add(TestCase("2.1.11", "notes.append", "PHASE_2", "HEADLESS_CORE", "Note Persistence", "Verifies notes persist across reads.", "note down test persistence", "Successfully appended and verified note", TestType.AUTOMATED, requiredPermission = "android.permission.WRITE_EXTERNAL_STORAGE"))
        add(TestCase("2.1.12", "timer.create", "PHASE_2", "HEADLESS_CORE", "Timer Command", "Dispatches system timer intent.", "timer 5", "Timer intent launched", TestType.AUTOMATED))
        add(TestCase("2.1.13", "alarm.create", "PHASE_2", "HEADLESS_CORE", "Alarm Command", "Dispatches system alarm intent.", "alarm 07:00", "Alarm intent launched", TestType.AUTOMATED))
        add(TestCase("2.1.14", "web.search", "PHASE_2", "HEADLESS_CORE", "Web Search", "Dispatches system web search intent.", "web search localagent", "Search intent launched", TestType.AUTOMATED))
        add(TestCase("2.1.15", "unknown.command", "PHASE_2", "HEADLESS_CORE", "Invalid Command", "Handles unrecognized command string safely.", "unsupported command xyz", "UNKNOWN_COMMAND", TestType.NEGATIVE))

        // APP LAUNCHING & DISCOVERY (8)
        add(TestCase("2.2.01", "app.launch", "PHASE_2", "APP_LAUNCH", "Launch Known Application", "Resolves and opens Settings app.", "open settings", "Settings app brought to foreground", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("2.2.02", "app.launch", "PHASE_2", "APP_LAUNCH", "Invalid Package", "Attempts to open non-existent app package.", "open NonExistentApp12345", "APP_NOT_FOUND", TestType.NEGATIVE))
        add(TestCase("2.2.03", "app.launch", "PHASE_2", "APP_LAUNCH", "Unavailable App Resolution", "Resolves ambiguous or missing app query.", "open FakeAppUnknown", "APP_NOT_FOUND", TestType.NEGATIVE))
        add(TestCase("2.2.04", "app.launch", "PHASE_2", "APP_LAUNCH", "Launcher Empty Query", "Handles empty app query string.", "open ", "INVALID_ARGUMENT", TestType.NEGATIVE))
        add(TestCase("2.5.APP.001", "app.current", "PHASE_2.5", "APPLICATION", "Current Foreground App Query", "Queries active foreground app package via usage stats.", "app current", "Foreground app package or USAGE_STATS restriction reported", TestType.AUTOMATED))
        add(TestCase("2.5.APP.002", "app.list", "PHASE_2.5", "APPLICATION", "List Installed Applications", "Lists launchable installed applications.", "app list", "Installed app list returned", TestType.AUTOMATED))
        add(TestCase("2.5.APP.003", "app.find", "PHASE_2.5", "APPLICATION", "Find App Positive (Settings)", "Finds Settings app package by query.", "app find settings", "Settings package matched", TestType.AUTOMATED))
        add(TestCase("2.5.APP.004", "app.find", "PHASE_2.5", "APPLICATION", "Find App Nonexistent Negative", "Attempts to find nonexistent app query.", "app find definitely_nonexistent_app_xyz", "APP_NOT_FOUND", TestType.NEGATIVE))
        add(TestCase("2.5.APP.005", "app.info", "PHASE_2.5", "APPLICATION", "App Info Positive (com.android.settings)", "Queries package info for Settings.", "app info com.android.settings", "Package info returned", TestType.AUTOMATED))
        add(TestCase("2.5.APP.006", "app.info", "PHASE_2.5", "APPLICATION", "App Info Nonexistent Negative", "Queries package info for nonexistent package.", "app info definitely.nonexistent.package", "NOT_FOUND", TestType.NEGATIVE))

        // FLASHLIGHT (5)
        add(TestCase("2.3.01", "flashlight.status", "PHASE_2", "FLASHLIGHT", "Flashlight Status", "Queries torch availability.", "flashlight status", "Flashlight status reported", TestType.AUTOMATED, requiredCapability = "FLASHLIGHT"))
        add(TestCase("2.3.02", "flashlight.on", "PHASE_2", "FLASHLIGHT", "Flashlight ON", "Turns flashlight ON physically.", "flashlight on", "Torch lights up physically", TestType.PHYSICAL, requiresPhysicalVerification = true, requiredCapability = "FLASHLIGHT"))
        add(TestCase("2.3.03", "flashlight.off", "PHASE_2", "FLASHLIGHT", "Flashlight OFF", "Turns flashlight OFF physically.", "flashlight off", "Torch turns off physically", TestType.PHYSICAL, requiresPhysicalVerification = true, requiredCapability = "FLASHLIGHT"))
        add(TestCase("2.3.04", "flashlight.on", "PHASE_2", "FLASHLIGHT", "Flashlight Repeated ON", "Turns flashlight ON when already ON.", "flashlight on", "Torch remains ON without error", TestType.PHYSICAL, requiresPhysicalVerification = true, requiredCapability = "FLASHLIGHT"))
        add(TestCase("2.3.05", "flashlight.off", "PHASE_2", "FLASHLIGHT", "Flashlight Repeated OFF", "Turns flashlight OFF when already OFF.", "flashlight off", "Torch remains OFF without error", TestType.PHYSICAL, requiresPhysicalVerification = true, requiredCapability = "FLASHLIGHT"))

        // HAPTICS (5)
        add(TestCase("2.3.06", "haptics.vibrate", "PHASE_2", "HAPTICS", "Normal Vibration", "Triggers 200ms haptic feedback.", "vibrate 200", "Device vibrates physically for 200ms", TestType.PHYSICAL, requiresPhysicalVerification = true, requiredCapability = "VIBRATION"))
        add(TestCase("2.3.07", "haptics.vibrate", "PHASE_2", "HAPTICS", "Minimum Allowed Vibration", "Triggers 1ms haptic feedback (min bound).", "vibrate 1", "Device vibrates briefly (1ms)", TestType.PHYSICAL, requiresPhysicalVerification = true, requiredCapability = "VIBRATION"))
        add(TestCase("2.3.08", "haptics.vibrate", "PHASE_2", "HAPTICS", "Maximum Allowed Vibration", "Triggers 2000ms haptic feedback (max bound).", "vibrate 2000", "Device vibrates physically for 2000ms", TestType.PHYSICAL, requiresPhysicalVerification = true, requiredCapability = "VIBRATION"))
        add(TestCase("2.3.09", "haptics.vibrate", "PHASE_2", "HAPTICS", "Invalid Vibration Duration", "Rejects duration exceeding 2000ms maximum.", "vibrate 5000", "INVALID_ARGUMENT", TestType.NEGATIVE))
        add(TestCase("2.3.10", "haptics.status", "PHASE_2", "HAPTICS", "Vibration Status", "Queries haptic status.", "vibrate status", "Vibration status reported", TestType.AUTOMATED, requiredCapability = "VIBRATION"))

        // CLIPBOARD (4)
        add(TestCase("2.5.CLIP.001", "clipboard.status", "PHASE_2.5", "CLIPBOARD", "Clipboard Status Query", "Queries system clipboard state.", "clipboard status", "Clipboard status reported", TestType.AUTOMATED))
        add(TestCase("2.5.CLIP.002", "clipboard.write", "PHASE_2.5", "CLIPBOARD", "Write Clipboard Text", "Writes test string to clipboard.", "clipboard write LocalAgent Test String", "Clipboard text written", TestType.AUTOMATED))
        add(TestCase("2.5.CLIP.003", "clipboard.read", "PHASE_2.5", "CLIPBOARD", "Read Clipboard Contents", "Reads current text from clipboard.", "clipboard read", "Clipboard content read or empty reported", TestType.AUTOMATED))
        add(TestCase("2.5.CLIP.004", "clipboard.clear", "PHASE_2.5", "CLIPBOARD", "Clear Clipboard Contents", "Clears system clipboard.", "clipboard clear", "Clipboard cleared", TestType.AUTOMATED))

        // NOTIFICATIONS (2)
        add(TestCase("2.5.NOTIF.001", "notification.status", "PHASE_2.5", "OBSERVATION", "Notification Listener Status Query", "Queries NotificationListener connection state.", "notification status", "Notification status reported", TestType.PERMISSION))
        add(TestCase("2.5.NOTIF.002", "notification.latest", "PHASE_2.5", "OBSERVATION", "Latest Notification Query", "Reads latest received notification snapshot.", "notification latest", "Latest notification reported or NO_DATA", TestType.AUTOMATED))

        // DISPLAY & INPUT (6)
        add(TestCase("2.5.DISP.001", "display.status", "PHASE_2.5", "DEVICE", "Display Status Query", "Queries screen metrics, density, and orientation.", "display status", "Display metrics reported", TestType.AUTOMATED))
        add(TestCase("2.5.DISP.002", "display.dimensions", "PHASE_2.5", "DEVICE", "Display Dimensions Query", "Queries screen pixel width and height.", "display dimensions", "Screen dimensions reported", TestType.AUTOMATED))
        add(TestCase("2.5.DISP.003", "display.orientation", "PHASE_2.5", "DEVICE", "Display Orientation Query", "Queries screen orientation.", "display orientation", "Screen orientation reported", TestType.AUTOMATED))
        add(TestCase("2.5.DISP.004", "screen.capture.status", "PHASE_2.5", "DEVICE", "Screen Capture Status Query", "Queries screen capture permission/consent state.", "screen capture status", "Screen capture status reported", TestType.AUTOMATED))
        add(TestCase("2.5.INP.001", "keyboard.status", "PHASE_2.5", "OBSERVATION", "Soft Keyboard Status Query", "Queries soft keyboard visibility.", "keyboard status", "Keyboard status reported", TestType.AUTOMATED))
        add(TestCase("2.5.INP.002", "input.status", "PHASE_2.5", "OBSERVATION", "Input Method Status Query", "Queries IME and input capability state.", "input status", "Input status reported", TestType.AUTOMATED))

        // CAMERA (3)
        add(TestCase("2.5.CAM.001", "camera.status", "PHASE_2.5", "DEVICE", "Camera Hardware Status Query", "Queries camera hardware count.", "camera status", "Camera count and availability reported", TestType.HARDWARE))
        add(TestCase("2.5.CAM.002", "camera.permission", "PHASE_2.5", "DEVICE", "Camera Permission Query", "Queries CAMERA runtime permission.", "camera permission", "CAMERA permission state reported", TestType.PERMISSION))
        add(TestCase("2.5.CAM.003", "camera.list", "PHASE_2.5", "DEVICE", "Camera Device List Query", "Lists installed camera device IDs.", "camera list", "Camera IDs listed", TestType.HARDWARE))

        // VOLUME STREAMS: MUSIC, RING, ALARM, NOTIFICATION (16)
        val streams = listOf("music", "ring", "alarm", "notification")
        var volIdx = 11
        for (s in streams) {
            add(TestCase("2.3.VOL.$volIdx.1", "volume.$s.status", "PHASE_2", "VOLUME", "${s.uppercase()} Stream Status", "Queries $s stream status, indices, and percentage.", "volume $s status", "Reports stream $s status", TestType.AUTOMATED))
            add(TestCase("2.3.VOL.$volIdx.2", "volume.$s.current", "PHASE_2", "VOLUME", "${s.uppercase()} Current Index", "Queries current $s stream index.", "volume $s current", "Reports current index", TestType.AUTOMATED))
            add(TestCase("2.3.VOL.$volIdx.3", "volume.$s.maximum", "PHASE_2", "VOLUME", "${s.uppercase()} Maximum Index", "Queries maximum $s stream index.", "volume $s maximum", "Reports maximum index", TestType.AUTOMATED))
            add(TestCase("2.3.VOL.$volIdx.4", "volume.$s.percentage", "PHASE_2", "VOLUME", "${s.uppercase()} Stream Percentage", "Queries current $s stream percentage.", "volume $s percentage", "Reports percentage", TestType.AUTOMATED))
            add(TestCase("2.3.VOL.$volIdx.5", "volume.$s.set", "PHASE_2", "VOLUME", "${s.uppercase()} Volume Set 50%", "Sets $s stream volume to 50%.", "volume $s 50", "Volume set to 50%", TestType.PHYSICAL, requiresPhysicalVerification = true))
            add(TestCase("2.3.VOL.$volIdx.6", "volume.$s.set", "PHASE_2", "VOLUME", "${s.uppercase()} Volume Invalid Negative", "Rejects negative percentage.", "volume $s -1", "INVALID_ARGUMENT", TestType.NEGATIVE))
            add(TestCase("2.3.VOL.$volIdx.7", "volume.$s.set", "PHASE_2", "VOLUME", "${s.uppercase()} Volume Invalid Excess", "Rejects percentage > 100.", "volume $s 101", "INVALID_ARGUMENT", TestType.NEGATIVE))
            add(TestCase("2.3.VOL.$volIdx.8", "volume.$s.set", "PHASE_2", "VOLUME", "${s.uppercase()} Volume Non-Numeric", "Rejects non-numeric percentage.", "volume $s abc", "INVALID_ARGUMENT", TestType.NEGATIVE))
            volIdx++
        }

        // PHYSICAL VOLUME BUTTON PASS-THROUGH TEST
        add(TestCase("VOLUME-PHYSICAL-001", "safety.panic", "PHASE_2", "VOLUME", "Physical Volume Buttons Control", "Verify physical Volume Up/Down buttons remain under normal Android system control.", "press physical volume up/down", "Android volume slider appears and changes volume normally", TestType.PHYSICAL, requiresPhysicalVerification = true))

        // CONNECTIVITY (7)
        add(TestCase("2.3.30", "wifi.status", "PHASE_2", "CONNECTIVITY", "Wi-Fi Status", "Queries Wi-Fi enabled status.", "wifi status", "Reports Wi-Fi status", TestType.AUTOMATED, requiredCapability = "WIFI"))
        add(TestCase("2.3.31", "wifi.on", "PHASE_2", "CONNECTIVITY", "Wi-Fi ON Request", "Requests Wi-Fi enablement.", "wifi on", "Wi-Fi ON requested or restricted note", TestType.AUTOMATED, requiredCapability = "WIFI"))
        add(TestCase("2.3.32", "wifi.off", "PHASE_2", "CONNECTIVITY", "Wi-Fi OFF Request", "Requests Wi-Fi disablement.", "wifi off", "Wi-Fi OFF requested or restricted note", TestType.AUTOMATED, requiredCapability = "WIFI"))
        add(TestCase("2.3.33", "bluetooth.status", "PHASE_2", "CONNECTIVITY", "Bluetooth Status", "Queries Bluetooth status.", "bluetooth status", "Reports Bluetooth status", TestType.AUTOMATED, requiredCapability = "BLUETOOTH"))
        add(TestCase("2.3.34", "bluetooth.on", "PHASE_2", "CONNECTIVITY", "Bluetooth ON Request", "Requests Bluetooth enablement.", "bluetooth on", "Bluetooth ON requested or intent opened", TestType.AUTOMATED, requiredCapability = "BLUETOOTH"))
        add(TestCase("2.3.35", "bluetooth.off", "PHASE_2", "CONNECTIVITY", "Bluetooth OFF Request", "Requests Bluetooth disablement.", "bluetooth off", "Bluetooth OFF requested or intent opened", TestType.AUTOMATED, requiredCapability = "BLUETOOTH"))
        add(TestCase("2.3.36", "bluetooth.status", "PHASE_2", "CONNECTIVITY", "Bluetooth Hardware Check", "Verifies Bluetooth adapter detection.", "bluetooth status", "Adapter present and reported", TestType.HARDWARE, requiredCapability = "BLUETOOTH"))

        // SYSTEM CONTROLS (10)
        add(TestCase("2.3.37", "brightness.status", "PHASE_2", "SYSTEM_CONTROLS", "Brightness Status", "Queries screen brightness level (read-only).", "brightness status", "Reports current screen brightness level", TestType.AUTOMATED))
        add(TestCase("2.3.38", "brightness.set", "PHASE_2", "SYSTEM_CONTROLS", "Brightness Control Valid", "Sets screen brightness level (50%).", "brightness 50", "Screen brightness changes to ~50%", TestType.PHYSICAL, requiresPhysicalVerification = true, requiredPermission = "android.permission.WRITE_SETTINGS"))
        add(TestCase("2.3.39", "brightness.set", "PHASE_2", "SYSTEM_CONTROLS", "Brightness Control Invalid Value (128)", "Rejects brightness percentage > 100%.", "brightness 128", "INVALID_ARGUMENT", TestType.NEGATIVE))
        add(TestCase("2.3.40", "brightness.status", "PHASE_2", "SYSTEM_CONTROLS", "WRITE_SETTINGS Detection", "Verifies special permission detection for system settings.", "brightness status", "Reports WRITE_SETTINGS permission status", TestType.PERMISSION, requiredPermission = "android.permission.WRITE_SETTINGS"))
        add(TestCase("2.3.41", "ringer.status", "PHASE_2", "SYSTEM_CONTROLS", "Ringer Status", "Queries current ringer mode.", "ringer status", "Reports current ringer mode", TestType.AUTOMATED))
        add(TestCase("2.3.42", "ringer.normal", "PHASE_2", "SYSTEM_CONTROLS", "Ringer Mode Control", "Sets ringer mode to NORMAL.", "ringer normal", "Ringer mode set to NORMAL", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("2.3.43", "ringer.silent", "PHASE_2", "SYSTEM_CONTROLS", "Notification Policy Detection", "Verifies Do Not Disturb access detection.", "ringer silent", "Reports notification policy access requirement", TestType.PERMISSION))
        add(TestCase("2.5.RING.001", "ringer.vibrate", "PHASE_2.5", "SYSTEM_CONTROLS", "Ringer Mode Vibrate Control", "Sets ringer mode to VIBRATE.", "ringer vibrate", "Ringer mode set to VIBRATE", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("2.3.44", "location.status", "PHASE_2", "SYSTEM_CONTROLS", "Location Status", "Queries GPS/location provider state.", "location status", "Reports location status & provider state", TestType.AUTOMATED))
        add(TestCase("2.3.45", "battery.status", "PHASE_2", "SYSTEM_CONTROLS", "Battery Status", "Queries battery level and power state.", "battery status", "Reports battery percentage and charging state", TestType.AUTOMATED))

        // NETWORK, LOCATION, POWER, BACKGROUND & DEVICE SNAPSHOT (5)
        add(TestCase("2.5.NET.001", "network.status", "PHASE_2.5", "OBSERVATION", "Network Connection Status", "Queries active network state.", "network status", "Active network state reported", TestType.AUTOMATED))
        add(TestCase("2.5.LOC.001", "location.providers", "PHASE_2.5", "OBSERVATION", "Location Providers Query", "Queries location provider details.", "location providers", "Location providers listed", TestType.AUTOMATED))
        add(TestCase("2.5.PWR.001", "power.status", "PHASE_2.5", "OBSERVATION", "Power Interactivity Status", "Queries screen interactive power state.", "power status", "Power interactivity reported", TestType.AUTOMATED))
        add(TestCase("2.5.BG.001", "background.policy", "PHASE_2.5", "DIAGNOSTICS", "Background Execution Policy Query", "Queries background execution policy.", "background policy", "Background execution policy reported", TestType.AUTOMATED))
        add(TestCase("2.5.SNAP.001", "device.snapshot", "PHASE_2.5", "DIAGNOSTICS", "Device Unified Snapshot Query", "Aggregates unified device state snapshot.", "device snapshot", "Device snapshot aggregated", TestType.AUTOMATED))

        // SENSORS (7)
        add(TestCase("2.3.46", "sensor.list", "PHASE_2", "SENSORS", "Sensor Enumeration", "Enumerates installed hardware sensors via SensorManager.", "sensor list", "Lists installed hardware sensors with metadata", TestType.AUTOMATED))
        add(TestCase("2.3.47", "sensor.accelerometer.sample", "PHASE_2", "SENSORS", "Accelerometer Sample", "Requests 3-axis accelerometer reading.", "sensor accelerometer", "Returns X, Y, Z acceleration data", TestType.SENSOR, requiredCapability = "ACCELEROMETER"))
        add(TestCase("2.3.48", "sensor.gyroscope.sample", "PHASE_2", "SENSORS", "Gyroscope Sample", "Requests 3-axis gyroscope reading.", "sensor gyroscope", "Returns rotation rate or UNAVAILABLE", TestType.SENSOR, requiredCapability = "GYROSCOPE"))
        add(TestCase("2.3.49", "sensor.proximity.sample", "PHASE_2", "SENSORS", "Proximity Sample", "Requests proximity distance reading.", "sensor proximity", "Returns distance (cm) and range metadata", TestType.SENSOR, requiredCapability = "PROXIMITY"))
        add(TestCase("2.3.50", "sensor.light.sample", "PHASE_2", "SENSORS", "Light Sensor Sample", "Requests ambient light level reading.", "sensor light", "Returns illuminance (lux)", TestType.SENSOR, requiredCapability = "LIGHT"))
        add(TestCase("2.3.51", "sensor.unknown", "PHASE_2", "SENSORS", "Unavailable Sensor Request", "Requests data from non-existent sensor.", "sensor fake_sensor", "NO_SENSOR", TestType.NEGATIVE))

        // SPEECH: STT & TTS (6)
        add(TestCase("2.4.01", "stt.status", "PHASE_2.4", "STT", "STT Engine Availability", "Queries Android SpeechRecognizer availability.", "stt status", "SpeechRecognizer AVAILABLE or UNAVAILABLE reported", TestType.HARDWARE, requiredCapability = "STT"))
        add(TestCase("2.4.02", "stt.listen", "PHASE_2.4", "STT", "STT Listening Test", "Starts built-in speech recognition listener.", "stt listen", "Recognizer listens and returns transcript or timeout", TestType.PHYSICAL, requiresPhysicalVerification = true, requiredPermission = "android.permission.RECORD_AUDIO", requiredCapability = "STT"))
        add(TestCase("2.5.STT.001", "stt.cancel", "PHASE_2.5", "STT", "STT Cancel Session", "Cancels active speech recognition session.", "stt cancel", "Active speech session cancelled or NO_ACTIVE_SESSION", TestType.AUTOMATED, requiredCapability = "STT"))
        add(TestCase("2.4.03", "tts.status", "PHASE_2.4", "TTS", "TTS Engine Availability", "Queries Android TextToSpeech engine status.", "tts status", "TextToSpeech initialized and AVAILABLE", TestType.HARDWARE, requiredCapability = "TTS"))
        add(TestCase("2.4.04", "tts.speak", "PHASE_2.4", "TTS", "TTS Speak Test", "Speaks phrase 'Foundation test successful'.", "speak Foundation test successful", "Speech output heard physically from speaker", TestType.PHYSICAL, requiresPhysicalVerification = true, requiredCapability = "TTS"))
        add(TestCase("2.5.TTS.001", "tts.stop", "PHASE_2.5", "TTS", "TTS Stop Output", "Stops active speech output.", "tts stop", "TTS speech stopped or idle", TestType.AUTOMATED, requiredCapability = "TTS"))

        // PERMISSIONS, CAPABILITIES, ACCESSIBILITY & DIAGNOSTICS (5)
        add(TestCase("2.5.DIAG.001", "permissions.status", "PHASE_2.5", "DIAGNOSTICS", "All Permissions Status Query", "Queries runtime permissions & special access.", "permissions status", "Permissions status reported", TestType.PERMISSION))
        add(TestCase("2.5.DIAG.002", "capabilities.status", "PHASE_2.5", "DIAGNOSTICS", "All Capabilities Status Query", "Queries hardware capability states.", "capabilities status", "Capabilities status reported", TestType.HARDWARE))
        add(TestCase("2.5.DIAG.003", "accessibility.status", "PHASE_2.5", "DIAGNOSTICS", "Accessibility Connection Query", "Queries accessibility service connection.", "accessibility status", "Accessibility status reported", TestType.PERMISSION))
        add(TestCase("2.5.DIAG.004", "diagnostics.status", "PHASE_2.5", "DIAGNOSTICS", "Device Diagnostics Query", "Runs device diagnostics and sensor inspection.", "diagnostics status", "Diagnostics report generated", TestType.AUTOMATED))
        add(TestCase("2.5.DIAG.005", "diagnostics.readiness", "PHASE_2.5", "DIAGNOSTICS", "Foundation Readiness Evaluation", "Evaluates deterministic foundation readiness.", "readiness status", "Foundation readiness evaluated", TestType.AUTOMATED))
    }

    private fun add(testCase: TestCase) {
        testCases.add(testCase)
    }

    fun getAllTestCases(): List<TestCase> = testCases.toList()

    fun getTestCaseById(id: String): TestCase? = testCases.find { it.id == id }

    fun getTestCasesByCategory(category: String): List<TestCase> = testCases.filter { it.category == category }

    fun getTestCasesByPhase(phase: String): List<TestCase> = testCases.filter { it.phase == phase }

    fun clearAllResults() {
        for (tc in testCases) {
            tc.status = TestStatus.PENDING
            tc.observedResult = null
            tc.error = null
            tc.timestamp = null
            tc.duration = null
            tc.evidenceReferences = emptyList()
        }
    }

    fun updateTestCase(
        id: String,
        status: TestStatus,
        observedResult: String?,
        error: String?,
        duration: Long?,
        evidenceReferences: List<String> = emptyList()
    ): Boolean {
        val tc = getTestCaseById(id) ?: return false
        tc.status = status
        tc.observedResult = observedResult
        tc.error = error
        tc.duration = duration
        tc.timestamp = System.currentTimeMillis()
        tc.evidenceReferences = evidenceReferences
        return true
    }

    fun getSummary(): RegistrySummary {
        val total = testCases.size
        val passed = testCases.count { it.status == TestStatus.PASSED }
        val failed = testCases.count { it.status == TestStatus.FAILED }
        val blocked = testCases.count { it.status == TestStatus.BLOCKED }
        val skipped = testCases.count { it.status == TestStatus.SKIPPED }
        val pending = testCases.count { it.status == TestStatus.PENDING }
        val running = testCases.count { it.status == TestStatus.RUNNING }
        return RegistrySummary(total, passed, failed, blocked, skipped, pending, running)
    }
}

data class RegistrySummary(
    val total: Int,
    val passed: Int,
    val failed: Int,
    val blocked: Int,
    val skipped: Int,
    val pending: Int,
    val running: Int
)
