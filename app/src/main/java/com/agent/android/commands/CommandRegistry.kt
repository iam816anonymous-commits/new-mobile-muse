package com.agent.android.commands

class CommandRegistry {

    private val registry: MutableMap<String, CommandDefinition> = mutableMapOf()

    init {
        registerAllCommands()
    }

    private fun registerAllCommands() {
        registry.clear()

        // 1. SAFETY & EXECUTION
        register(CommandDefinition("safety.status", "Safety Status", CommandCategory.SAFETY, "Queries safety controller status", CommandStatus.IMPLEMENTED, "safety status", listOf("safety status"), handlerIdentifier = "ExecutionController"))
        register(CommandDefinition("safety.cancel", "Safety Cancel", CommandCategory.SAFETY, "Cancels current active execution", CommandStatus.IMPLEMENTED, "safety cancel", listOf("safety cancel"), handlerIdentifier = "ExecutionController"))
        register(CommandDefinition("safety.panic", "Panic Stop", CommandCategory.SAFETY, "Triggers emergency stop panic stop", CommandStatus.IMPLEMENTED, "panic", listOf("panic"), requirement = CommandRequirement(accessibilityRequired = true, physicalObservationRequired = true), handlerIdentifier = "LocalAgentAccessibilityService"))

        // 2. CALCULATOR
        register(CommandDefinition("calculator.calculate", "Calculate Math Expression", CommandCategory.HEADLESS_CORE, "Evaluates arithmetic math expressions", CommandStatus.IMPLEMENTED, "calculate <expression>", listOf("calculate 12 + 34", "calculate (2 + 3) * 4"), listOf("expression"), handlerIdentifier = "CalculatorSkill"))

        // 3. NOTES
        register(CommandDefinition("notes.append", "Append Note", CommandCategory.HEADLESS_CORE, "Appends text entry to persistent note file", CommandStatus.IMPLEMENTED, "note down <text>", listOf("note down buy milk"), listOf("text"), CommandRequirement(requiredPermissions = listOf("android.permission.WRITE_EXTERNAL_STORAGE"), changesDeviceState = true), handlerIdentifier = "NotesSkill"))

        // 4. TIMER / ALARM
        register(CommandDefinition("timer.create", "Set Timer", CommandCategory.HEADLESS_CORE, "Launches system timer intent for specified seconds", CommandStatus.IMPLEMENTED, "timer <seconds>", listOf("timer 60"), listOf("seconds"), CommandRequirement(launchesApplication = true), handlerIdentifier = "IntentSkills"))
        register(CommandDefinition("alarm.create", "Set Alarm", CommandCategory.HEADLESS_CORE, "Launches system alarm intent for HH:MM time", CommandStatus.IMPLEMENTED, "alarm <time>", listOf("alarm 07:30", "set alarm 07:30"), listOf("time"), CommandRequirement(launchesApplication = true), handlerIdentifier = "IntentSkills"))

        // 5. WEB SEARCH
        register(CommandDefinition("web.search", "Web Search", CommandCategory.HEADLESS_CORE, "Launches web search intent for query", CommandStatus.IMPLEMENTED, "web search <query>", listOf("web search localagent", "search localagent"), listOf("query"), CommandRequirement(launchesApplication = true), handlerIdentifier = "IntentSkills"))

        // 6. APP LAUNCHING
        register(CommandDefinition("app.launch", "Launch Application", CommandCategory.APPLICATION, "Resolves and opens installed application", CommandStatus.IMPLEMENTED, "open <app_name>", listOf("open settings", "launch settings"), listOf("query"), CommandRequirement(launchesApplication = true, physicalObservationRequired = true), handlerIdentifier = "AppLauncherImpl"))

        // 7. FLASHLIGHT
        register(CommandDefinition("flashlight.status", "Flashlight Status", CommandCategory.DEVICE, "Queries camera torch state", CommandStatus.IMPLEMENTED, "flashlight status", listOf("flashlight status"), requirement = CommandRequirement(requiredCapabilities = listOf("FLASHLIGHT")), handlerIdentifier = "FlashlightController"))
        register(CommandDefinition("flashlight.on", "Flashlight ON", CommandCategory.DEVICE, "Turns camera torch ON", CommandStatus.IMPLEMENTED, "flashlight on", listOf("flashlight on"), requirement = CommandRequirement(requiredPermissions = listOf("android.permission.CAMERA"), requiredCapabilities = listOf("FLASHLIGHT"), physicalObservationRequired = true, changesDeviceState = true), handlerIdentifier = "FlashlightController"))
        register(CommandDefinition("flashlight.off", "Flashlight OFF", CommandCategory.DEVICE, "Turns camera torch OFF", CommandStatus.IMPLEMENTED, "flashlight off", listOf("flashlight off"), requirement = CommandRequirement(requiredPermissions = listOf("android.permission.CAMERA"), requiredCapabilities = listOf("FLASHLIGHT"), physicalObservationRequired = true, changesDeviceState = true), handlerIdentifier = "FlashlightController"))

        // 8. HAPTICS
        register(CommandDefinition("haptics.status", "Haptics Status", CommandCategory.DEVICE, "Queries vibrator service status", CommandStatus.IMPLEMENTED, "vibrate status", listOf("vibrate status"), requirement = CommandRequirement(requiredCapabilities = listOf("VIBRATION")), handlerIdentifier = "HapticController"))
        register(CommandDefinition("haptics.vibrate", "Trigger Vibration", CommandCategory.DEVICE, "Triggers haptic vibration for duration (1-2000ms)", CommandStatus.IMPLEMENTED, "vibrate <duration_ms>", listOf("vibrate 200"), listOf("durationMs"), CommandRequirement(requiredCapabilities = listOf("VIBRATION"), physicalObservationRequired = true), handlerIdentifier = "HapticController"))

        // 9. VOLUME STREAMS (ALARM, RING, NOTIFICATION, MUSIC)
        val streams = listOf("alarm", "ring", "notification", "music")
        for (s in streams) {
            register(CommandDefinition("volume.$s.status", "${s.uppercase()} Volume Status", CommandCategory.DEVICE, "Queries complete $s volume status", CommandStatus.IMPLEMENTED, "volume $s status", listOf("volume $s status"), handlerIdentifier = "VolumeController"))
            register(CommandDefinition("volume.$s.current", "${s.uppercase()} Volume Current", CommandCategory.DEVICE, "Queries current $s volume index", CommandStatus.IMPLEMENTED, "volume $s current", listOf("volume $s current"), handlerIdentifier = "VolumeController"))
            register(CommandDefinition("volume.$s.maximum", "${s.uppercase()} Volume Maximum", CommandCategory.DEVICE, "Queries maximum $s volume index", CommandStatus.IMPLEMENTED, "volume $s maximum", listOf("volume $s maximum"), handlerIdentifier = "VolumeController"))
            register(CommandDefinition("volume.$s.percentage", "${s.uppercase()} Volume Percentage", CommandCategory.DEVICE, "Queries current $s volume percentage", CommandStatus.IMPLEMENTED, "volume $s percentage", listOf("volume $s percentage"), handlerIdentifier = "VolumeController"))
            register(CommandDefinition("volume.$s.set", "Set ${s.uppercase()} Volume", CommandCategory.DEVICE, "Sets $s stream volume percentage (0-100%)", CommandStatus.IMPLEMENTED, "volume $s <percentage>", listOf("volume $s 50"), listOf("percentage"), CommandRequirement(physicalObservationRequired = true, changesDeviceState = true), handlerIdentifier = "VolumeController"))
        }

        // 10. CONNECTIVITY
        register(CommandDefinition("wifi.status", "Wi-Fi Status", CommandCategory.CONNECTIVITY, "Queries Wi-Fi hardware enabled status", CommandStatus.IMPLEMENTED, "wifi status", listOf("wifi status"), requirement = CommandRequirement(requiredCapabilities = listOf("WIFI")), handlerIdentifier = "ConnectivityControllers"))
        register(CommandDefinition("wifi.on", "Wi-Fi Enable Request", CommandCategory.CONNECTIVITY, "Requests Wi-Fi enablement", CommandStatus.IMPLEMENTED, "wifi on", listOf("wifi on"), requirement = CommandRequirement(requiredCapabilities = listOf("WIFI")), handlerIdentifier = "ConnectivityControllers"))
        register(CommandDefinition("wifi.off", "Wi-Fi Disable Request", CommandCategory.CONNECTIVITY, "Requests Wi-Fi disablement", CommandStatus.IMPLEMENTED, "wifi off", listOf("wifi off"), requirement = CommandRequirement(requiredCapabilities = listOf("WIFI")), handlerIdentifier = "ConnectivityControllers"))
        register(CommandDefinition("bluetooth.status", "Bluetooth Status", CommandCategory.CONNECTIVITY, "Queries Bluetooth adapter status", CommandStatus.IMPLEMENTED, "bluetooth status", listOf("bluetooth status"), requirement = CommandRequirement(requiredCapabilities = listOf("BLUETOOTH")), handlerIdentifier = "ConnectivityControllers"))
        register(CommandDefinition("bluetooth.on", "Bluetooth Enable Request", CommandCategory.CONNECTIVITY, "Requests Bluetooth enablement", CommandStatus.IMPLEMENTED, "bluetooth on", listOf("bluetooth on"), requirement = CommandRequirement(requiredCapabilities = listOf("BLUETOOTH")), handlerIdentifier = "ConnectivityControllers"))
        register(CommandDefinition("bluetooth.off", "Bluetooth Disable Request", CommandCategory.CONNECTIVITY, "Requests Bluetooth disablement", CommandStatus.IMPLEMENTED, "bluetooth off", listOf("bluetooth off"), requirement = CommandRequirement(requiredCapabilities = listOf("BLUETOOTH")), handlerIdentifier = "ConnectivityControllers"))

        // 11. BATTERY
        register(CommandDefinition("battery.status", "Battery Status", CommandCategory.OBSERVATION, "Queries battery level and power charging state", CommandStatus.IMPLEMENTED, "battery status", listOf("battery status"), handlerIdentifier = "SystemControlControllers"))

        // 12. SENSORS
        register(CommandDefinition("sensor.list", "List Hardware Sensors", CommandCategory.OBSERVATION, "Enumerates hardware sensors on device via SensorManager", CommandStatus.IMPLEMENTED, "sensor list", listOf("sensor list"), handlerIdentifier = "HardwareObservationControllers"))
        register(CommandDefinition("sensor.accelerometer.sample", "Accelerometer Reading", CommandCategory.OBSERVATION, "Samples 3-axis accelerometer values", CommandStatus.IMPLEMENTED, "sensor accelerometer", listOf("sensor accelerometer"), requirement = CommandRequirement(requiredCapabilities = listOf("ACCELEROMETER")), handlerIdentifier = "HardwareObservationControllers"))
        register(CommandDefinition("sensor.gyroscope.sample", "Gyroscope Reading", CommandCategory.OBSERVATION, "Samples 3-axis gyroscope values", CommandStatus.IMPLEMENTED, "sensor gyroscope", listOf("sensor gyroscope"), requirement = CommandRequirement(requiredCapabilities = listOf("GYROSCOPE")), handlerIdentifier = "HardwareObservationControllers"))
        register(CommandDefinition("sensor.proximity.sample", "Proximity Reading", CommandCategory.OBSERVATION, "Samples proximity sensor distance (cm)", CommandStatus.IMPLEMENTED, "sensor proximity", listOf("sensor proximity"), requirement = CommandRequirement(requiredCapabilities = listOf("PROXIMITY"), physicalObservationRequired = true), handlerIdentifier = "HardwareObservationControllers"))
        register(CommandDefinition("sensor.light.sample", "Light Sensor Reading", CommandCategory.OBSERVATION, "Samples ambient light illuminance (lux)", CommandStatus.IMPLEMENTED, "sensor light", listOf("sensor light"), requirement = CommandRequirement(requiredCapabilities = listOf("LIGHT"), physicalObservationRequired = true), handlerIdentifier = "HardwareObservationControllers"))

        // 13. SYSTEM CONTROLS
        register(CommandDefinition("brightness.status", "Brightness Status", CommandCategory.SYSTEM_CONTROLS, "Queries screen brightness level", CommandStatus.IMPLEMENTED, "brightness status", listOf("brightness status"), handlerIdentifier = "SystemControlControllers"))
        register(CommandDefinition("brightness.set", "Set Brightness", CommandCategory.SYSTEM_CONTROLS, "Sets screen brightness (0-100%)", CommandStatus.IMPLEMENTED, "brightness <percentage>", listOf("brightness 50"), listOf("percentage"), CommandRequirement(requiredSpecialAccess = listOf("android.permission.WRITE_SETTINGS"), physicalObservationRequired = true, changesDeviceState = true), handlerIdentifier = "SystemControlControllers"))
        register(CommandDefinition("ringer.status", "Ringer Mode Status", CommandCategory.SYSTEM_CONTROLS, "Queries current ringer mode", CommandStatus.IMPLEMENTED, "ringer status", listOf("ringer status"), handlerIdentifier = "SystemControlControllers"))
        register(CommandDefinition("ringer.normal", "Ringer Mode Normal", CommandCategory.SYSTEM_CONTROLS, "Sets ringer mode to NORMAL", CommandStatus.IMPLEMENTED, "ringer normal", listOf("ringer normal"), requirement = CommandRequirement(physicalObservationRequired = true, changesDeviceState = true), handlerIdentifier = "SystemControlControllers"))
        register(CommandDefinition("ringer.vibrate", "Ringer Mode Vibrate", CommandCategory.SYSTEM_CONTROLS, "Sets ringer mode to VIBRATE", CommandStatus.IMPLEMENTED, "ringer vibrate", listOf("ringer vibrate"), requirement = CommandRequirement(physicalObservationRequired = true, changesDeviceState = true), handlerIdentifier = "SystemControlControllers"))
        register(CommandDefinition("ringer.silent", "Ringer Mode Silent", CommandCategory.SYSTEM_CONTROLS, "Sets ringer mode to SILENT", CommandStatus.IMPLEMENTED, "ringer silent", listOf("ringer silent"), requirement = CommandRequirement(requiredSpecialAccess = listOf("Notification Policy Access"), physicalObservationRequired = true, changesDeviceState = true), handlerIdentifier = "SystemControlControllers"))
        register(CommandDefinition("location.status", "Location Status", CommandCategory.SYSTEM_CONTROLS, "Queries location provider states", CommandStatus.IMPLEMENTED, "location status", listOf("location status"), handlerIdentifier = "SystemControlControllers"))

        // 14. SPEECH (STT / TTS)
        register(CommandDefinition("stt.status", "STT Availability", CommandCategory.SPEECH, "Queries SpeechRecognizer availability", CommandStatus.IMPLEMENTED, "stt status", listOf("stt status"), requirement = CommandRequirement(requiredCapabilities = listOf("STT")), handlerIdentifier = "SpeechToTextEngine"))
        register(CommandDefinition("stt.listen", "STT Listen", CommandCategory.SPEECH, "Starts built-in speech recognition listener", CommandStatus.IMPLEMENTED, "stt listen", listOf("stt listen"), requirement = CommandRequirement(requiredPermissions = listOf("android.permission.RECORD_AUDIO"), requiredCapabilities = listOf("STT"), physicalObservationRequired = true), handlerIdentifier = "SpeechToTextEngine"))
        register(CommandDefinition("stt.cancel", "STT Cancel", CommandCategory.SPEECH, "Cancels active speech recognition", CommandStatus.IMPLEMENTED, "stt cancel", listOf("stt cancel"), requirement = CommandRequirement(requiredCapabilities = listOf("STT")), handlerIdentifier = "SpeechToTextEngine"))
        register(CommandDefinition("tts.status", "TTS Availability", CommandCategory.SPEECH, "Queries TextToSpeech availability", CommandStatus.IMPLEMENTED, "tts status", listOf("tts status"), requirement = CommandRequirement(requiredCapabilities = listOf("TTS")), handlerIdentifier = "TextToSpeechEngine"))
        register(CommandDefinition("tts.speak", "TTS Speak", CommandCategory.SPEECH, "Speaks text string using built-in TTS", CommandStatus.IMPLEMENTED, "speak <text>", listOf("speak Foundation test successful"), listOf("text"), CommandRequirement(requiredCapabilities = listOf("TTS"), physicalObservationRequired = true), handlerIdentifier = "TextToSpeechEngine"))
        register(CommandDefinition("tts.stop", "TTS Stop", CommandCategory.SPEECH, "Stops active speech output", CommandStatus.IMPLEMENTED, "tts stop", listOf("tts stop"), requirement = CommandRequirement(requiredCapabilities = listOf("TTS")), handlerIdentifier = "TextToSpeechEngine"))

        // 15. PERMISSIONS / DIAGNOSTICS
        register(CommandDefinition("permissions.status", "Permissions Status", CommandCategory.DIAGNOSTICS, "Queries all runtime permissions & special access status", CommandStatus.IMPLEMENTED, "permissions status", listOf("permissions status"), handlerIdentifier = "CapabilityRegistry"))
        register(CommandDefinition("capabilities.status", "Capabilities Status", CommandCategory.DIAGNOSTICS, "Queries all hardware capability states", CommandStatus.IMPLEMENTED, "capabilities status", listOf("capabilities status"), handlerIdentifier = "CapabilityRegistry"))
        register(CommandDefinition("accessibility.status", "Accessibility Status", CommandCategory.DIAGNOSTICS, "Queries accessibility service connection status", CommandStatus.IMPLEMENTED, "accessibility status", listOf("accessibility status"), handlerIdentifier = "LocalAgentAccessibilityService"))
        register(CommandDefinition("diagnostics.status", "Diagnostics Report", CommandCategory.DIAGNOSTICS, "Runs device diagnostics and sensor inspection", CommandStatus.IMPLEMENTED, "diagnostics status", listOf("diagnostics status", "device info"), handlerIdentifier = "SystemControlControllers"))
        register(CommandDefinition("diagnostics.readiness", "Readiness Evaluation", CommandCategory.DIAGNOSTICS, "Evaluates deterministic foundation readiness", CommandStatus.IMPLEMENTED, "readiness status", listOf("readiness status"), handlerIdentifier = "FoundationReadinessEvaluator"))
    }

    fun register(command: CommandDefinition) {
        registry[command.commandId] = command
    }

    fun getAllCommands(): List<CommandDefinition> = registry.values.toList()

    fun getCommandById(commandId: String): CommandDefinition? = registry[commandId]

    fun findCommandForInput(rawInput: String): CommandDefinition? {
        val trimmed = rawInput.trim().lowercase()
        if (trimmed.isEmpty()) return null
        val inputTokens = trimmed.split("\\s+".toRegex())

        // Sort commands descending by fixed token count (longest/most specific prefix first)
        val sortedCmds = registry.values.sortedByDescending { cmd ->
            cmd.syntax.lowercase().split("\\s+".toRegex()).takeWhile { !it.startsWith("<") }.size
        }

        for (cmd in sortedCmds) {
            val syntaxTokens = cmd.syntax.lowercase().split("\\s+".toRegex())
            val fixedTokens = syntaxTokens.takeWhile { !it.startsWith("<") }
            val hasVariable = syntaxTokens.size > fixedTokens.size

            if (inputTokens.size < fixedTokens.size) continue

            var matchesFixed = true
            for (i in fixedTokens.indices) {
                if (inputTokens[i] != fixedTokens[i]) {
                    matchesFixed = false
                    break
                }
            }

            if (!matchesFixed) continue

            if (!hasVariable) {
                if (inputTokens.size == fixedTokens.size) {
                    return cmd
                }
            } else {
                return cmd
            }
        }

        return registry.values.find { cmd ->
            cmd.examples.any { ex -> trimmed.startsWith(ex.lowercase()) || trimmed == ex.lowercase() }
        }
    }

    fun parseArguments(rawInput: String, definition: CommandDefinition): CommandArguments {
        val params = mutableMapOf<String, String>()
        val trimmed = rawInput.trim()
        val parts = trimmed.split("\\s+".toRegex())

        when (definition.commandId) {
            "calculator.calculate" -> {
                if (trimmed.length > "calculate".length) {
                    params["expression"] = trimmed.substring("calculate".length).trim()
                }
            }
            "notes.append" -> {
                if (trimmed.length > "note down".length) {
                    params["text"] = trimmed.substring("note down".length).trim()
                } else if (trimmed.length > "note append".length) {
                    params["text"] = trimmed.substring("note append".length).trim()
                }
            }
            "timer.create" -> {
                if (parts.size >= 2) params["seconds"] = parts[1]
            }
            "alarm.create" -> {
                val timeArg = if (trimmed.startsWith("set alarm")) trimmed.substringAfter("set alarm").trim() else trimmed.substringAfter("alarm").trim()
                params["time"] = timeArg
            }
            "web.search" -> {
                val qArg = if (trimmed.startsWith("web search")) trimmed.substringAfter("web search").trim() else trimmed.substringAfter("search").trim()
                params["query"] = qArg
            }
            "app.launch" -> {
                val qArg = if (trimmed.startsWith("open")) trimmed.substringAfter("open").trim() else trimmed.substringAfter("launch").trim()
                params["query"] = qArg
            }
            "haptics.vibrate" -> {
                if (parts.size >= 2) params["durationMs"] = parts[1]
            }
            "brightness.set" -> {
                if (parts.size >= 2) params["value"] = parts[1]
            }
            "tts.speak" -> {
                if (trimmed.length > "speak".length) {
                    params["text"] = trimmed.substring("speak".length).trim()
                }
            }
            else -> {
                if (definition.commandId.startsWith("volume.") && definition.commandId.endsWith(".set")) {
                    if (parts.size >= 3) {
                        params["percentage"] = parts[2]
                    }
                } else if (parts.size > 1) {
                    params["rawArgs"] = parts.drop(1).joinToString(" ")
                }
            }
        }

        return CommandArguments(
            commandId = definition.commandId,
            rawCommand = rawInput,
            params = params
        )
    }
}
