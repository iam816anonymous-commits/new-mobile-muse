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

        // APP LAUNCHING (4)
        add(TestCase("2.2.01", "app.launch", "PHASE_2", "APP_LAUNCH", "Launch Known Application", "Resolves and opens Settings app.", "open settings", "Settings app brought to foreground", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("2.2.02", "app.launch", "PHASE_2", "APP_LAUNCH", "Invalid Package", "Attempts to open non-existent app package.", "open NonExistentApp12345", "APP_NOT_FOUND", TestType.NEGATIVE))
        add(TestCase("2.2.03", "app.launch", "PHASE_2", "APP_LAUNCH", "Unavailable App Resolution", "Resolves ambiguous or missing app query.", "open FakeAppUnknown", "APP_NOT_FOUND", TestType.NEGATIVE))
        add(TestCase("2.2.04", "app.launch", "PHASE_2", "APP_LAUNCH", "Launcher Empty Query", "Handles empty app query string.", "open ", "INVALID_ARGUMENT", TestType.NEGATIVE))

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

        // VOLUME (9)
        add(TestCase("2.3.11", "volume.music.status", "PHASE_2", "VOLUME", "Media Current Volume", "Queries current music volume stream level.", "volume music status", "Returns current/max music volume", TestType.AUTOMATED))
        add(TestCase("2.3.12", "volume.music.set", "PHASE_2", "VOLUME", "Media Volume Low (10%)", "Sets music stream volume to 10%.", "volume music 10", "Music volume changes to ~10%", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("2.3.13", "volume.music.set", "PHASE_2", "VOLUME", "Media Volume Medium (50%)", "Sets music stream volume to 50%.", "volume music 50", "Music volume changes to ~50%", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("2.3.14", "volume.music.set", "PHASE_2", "VOLUME", "Media Volume High (90%)", "Sets music stream volume to 90%.", "volume music 90", "Music volume changes to ~90%", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("2.3.15", "volume.notification.set", "PHASE_2", "VOLUME", "Notification Volume Control", "Sets notification stream volume to 50%.", "volume notification 50", "Notification volume changes to ~50%", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("2.3.16", "volume.ring.set", "PHASE_2", "VOLUME", "Ring Volume Control", "Sets ringer stream volume to 50%.", "volume ring 50", "Ring volume changes to ~50%", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("2.3.17", "volume.alarm.set", "PHASE_2", "VOLUME", "Alarm Volume Control", "Sets alarm stream volume to 50%.", "volume alarm 50", "Alarm volume changes to ~50%", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("2.3.18", "volume.system.set", "PHASE_2", "VOLUME", "System Volume Control", "Sets system stream volume to 50%.", "volume system 50", "System volume changes to ~50%", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("2.3.19", "volume.music.set", "PHASE_2", "VOLUME", "Invalid Volume Percentage", "Rejects volume percentage > 100%.", "volume music 150", "INVALID_ARGUMENT", TestType.NEGATIVE))

        // CONNECTIVITY (7)
        add(TestCase("2.3.20", "wifi.status", "PHASE_2", "CONNECTIVITY", "Wi-Fi Status", "Queries Wi-Fi enabled status.", "wifi status", "Reports Wi-Fi status", TestType.AUTOMATED, requiredCapability = "WIFI"))
        add(TestCase("2.3.21", "wifi.on", "PHASE_2", "CONNECTIVITY", "Wi-Fi ON Request", "Requests Wi-Fi enablement.", "wifi on", "Wi-Fi ON requested or restricted note", TestType.AUTOMATED, requiredCapability = "WIFI"))
        add(TestCase("2.3.22", "wifi.off", "PHASE_2", "CONNECTIVITY", "Wi-Fi OFF Request", "Requests Wi-Fi disablement.", "wifi off", "Wi-Fi OFF requested or restricted note", TestType.AUTOMATED, requiredCapability = "WIFI"))
        add(TestCase("2.3.23", "bluetooth.status", "PHASE_2", "CONNECTIVITY", "Bluetooth Status", "Queries Bluetooth status.", "bluetooth status", "Reports Bluetooth status", TestType.AUTOMATED, requiredCapability = "BLUETOOTH"))
        add(TestCase("2.3.24", "bluetooth.on", "PHASE_2", "CONNECTIVITY", "Bluetooth ON Request", "Requests Bluetooth enablement.", "bluetooth on", "Bluetooth ON requested or intent opened", TestType.AUTOMATED, requiredCapability = "BLUETOOTH"))
        add(TestCase("2.3.25", "bluetooth.off", "PHASE_2", "CONNECTIVITY", "Bluetooth OFF Request", "Requests Bluetooth disablement.", "bluetooth off", "Bluetooth OFF requested or intent opened", TestType.AUTOMATED, requiredCapability = "BLUETOOTH"))
        add(TestCase("2.3.26", "bluetooth.status", "PHASE_2", "CONNECTIVITY", "Bluetooth Hardware Check", "Verifies Bluetooth adapter detection.", "bluetooth status", "Adapter present and reported", TestType.HARDWARE, requiredCapability = "BLUETOOTH"))

        // SYSTEM CONTROLS (9)
        add(TestCase("2.3.27", "brightness.status", "PHASE_2", "SYSTEM_CONTROLS", "Brightness Status", "Queries screen brightness level (read-only).", "brightness status", "Reports current screen brightness level", TestType.AUTOMATED))
        add(TestCase("2.3.28", "brightness.set", "PHASE_2", "SYSTEM_CONTROLS", "Brightness Control Valid", "Sets screen brightness level (50%).", "brightness 50", "Screen brightness changes to ~50%", TestType.PHYSICAL, requiresPhysicalVerification = true, requiredPermission = "android.permission.WRITE_SETTINGS"))
        add(TestCase("2.3.29", "brightness.set", "PHASE_2", "SYSTEM_CONTROLS", "Brightness Control Invalid Value (128)", "Rejects brightness percentage > 100%.", "brightness 128", "INVALID_ARGUMENT", TestType.NEGATIVE))
        add(TestCase("2.3.30", "brightness.status", "PHASE_2", "SYSTEM_CONTROLS", "WRITE_SETTINGS Detection", "Verifies special permission detection for system settings.", "brightness status", "Reports WRITE_SETTINGS permission status", TestType.PERMISSION, requiredPermission = "android.permission.WRITE_SETTINGS"))
        add(TestCase("2.3.31", "ringer.status", "PHASE_2", "SYSTEM_CONTROLS", "Ringer Status", "Queries current ringer mode.", "ringer status", "Reports current ringer mode", TestType.AUTOMATED))
        add(TestCase("2.3.32", "ringer.normal", "PHASE_2", "SYSTEM_CONTROLS", "Ringer Mode Control", "Sets ringer mode to NORMAL.", "ringer normal", "Ringer mode set to NORMAL", TestType.PHYSICAL, requiresPhysicalVerification = true))
        add(TestCase("2.3.33", "ringer.silent", "PHASE_2", "SYSTEM_CONTROLS", "Notification Policy Detection", "Verifies Do Not Disturb access detection.", "ringer silent", "Reports notification policy access requirement", TestType.PERMISSION))
        add(TestCase("2.3.34", "location.status", "PHASE_2", "SYSTEM_CONTROLS", "Location Status", "Queries GPS/location provider state.", "location status", "Reports location status & provider state", TestType.AUTOMATED))
        add(TestCase("2.3.35", "battery.status", "PHASE_2", "SYSTEM_CONTROLS", "Battery Status", "Queries battery level and power state.", "battery status", "Reports battery percentage and charging state", TestType.AUTOMATED))

        // SENSORS (7)
        add(TestCase("2.3.36", "sensor.list", "PHASE_2", "SENSORS", "Sensor Enumeration", "Enumerates installed hardware sensors via SensorManager.", "sensor list", "Lists installed hardware sensors with metadata", TestType.AUTOMATED))
        add(TestCase("2.3.37", "sensor.accelerometer.sample", "PHASE_2", "SENSORS", "Accelerometer Sample", "Requests 3-axis accelerometer reading.", "sensor accelerometer", "Returns X, Y, Z acceleration data", TestType.SENSOR, requiredCapability = "ACCELEROMETER"))
        add(TestCase("2.3.38", "sensor.gyroscope.sample", "PHASE_2", "SENSORS", "Gyroscope Sample", "Requests 3-axis gyroscope reading.", "sensor gyroscope", "Returns rotation rate or UNAVAILABLE", TestType.SENSOR, requiredCapability = "GYROSCOPE"))
        add(TestCase("2.3.39", "sensor.proximity.sample", "PHASE_2", "SENSORS", "Proximity Sample", "Requests proximity distance reading.", "sensor proximity", "Returns distance (cm) and range metadata", TestType.SENSOR, requiredCapability = "PROXIMITY"))
        add(TestCase("2.3.40", "sensor.light.sample", "PHASE_2", "SENSORS", "Light Sensor Sample", "Requests ambient light level reading.", "sensor light", "Returns illuminance (lux)", TestType.SENSOR, requiredCapability = "LIGHT"))
        add(TestCase("2.3.41", "sensor.unknown", "PHASE_2", "SENSORS", "Unavailable Sensor Request", "Requests data from non-existent sensor.", "sensor fake_sensor", "NO_SENSOR", TestType.NEGATIVE))

        // STT & TTS (4)
        add(TestCase("2.4.01", "stt.status", "PHASE_2.4", "STT", "STT Engine Availability", "Queries Android SpeechRecognizer availability.", null, "SpeechRecognizer AVAILABLE or UNAVAILABLE reported", TestType.HARDWARE, requiredCapability = "STT"))
        add(TestCase("2.4.02", "stt.listen", "PHASE_2.4", "STT", "STT Listening Test", "Starts built-in speech recognition listener.", null, "Recognizer listens and returns transcript or timeout", TestType.PHYSICAL, requiresPhysicalVerification = true, requiredPermission = "android.permission.RECORD_AUDIO", requiredCapability = "STT"))
        add(TestCase("2.4.03", "tts.status", "PHASE_2.4", "TTS", "TTS Engine Availability", "Queries Android TextToSpeech engine status.", null, "TextToSpeech initialized and AVAILABLE", TestType.HARDWARE, requiredCapability = "TTS"))
        add(TestCase("2.4.04", "tts.speak", "PHASE_2.4", "TTS", "TTS Speak Test", "Speaks phrase 'Foundation test successful'.", "speak Foundation test successful", "Speech output heard physically from speaker", TestType.PHYSICAL, requiresPhysicalVerification = true, requiredCapability = "TTS"))
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
