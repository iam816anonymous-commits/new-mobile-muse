package com.agent.android.execution

import com.agent.android.agent.device.ConnectivityControllers
import com.agent.android.agent.device.FlashlightController
import com.agent.android.agent.device.HapticController
import com.agent.android.agent.device.HardwareObservationControllers
import com.agent.android.agent.device.SystemControlControllers
import com.agent.android.agent.device.VolumeController
import com.agent.android.agent.skills.CalculatorSkill
import com.agent.android.agent.skills.IntentSkills
import com.agent.android.agent.skills.NotesSkill
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus
import com.agent.android.agent.skills.app.AppLaunchStatus
import com.agent.android.agent.skills.app.AppLauncher
import com.agent.android.commands.CommandRegistry
import com.agent.android.commands.CommandStatus
import com.agent.android.safety.CancellationReason

data class DispatchDetails(
    val command: String,
    val operation: String,
    val controllerName: String,
    val result: SkillResult,
    val verificationText: String
)

class GoalDispatcherImpl(
    val executionController: ExecutionController,
    private val calculatorSkill: CalculatorSkill = CalculatorSkill(),
    private val notesSkill: NotesSkill = NotesSkill(),
    private val intentSkills: IntentSkills? = null,
    private val flashlightController: FlashlightController? = null,
    private val hapticController: HapticController? = null,
    private val volumeController: VolumeController? = null,
    private val connectivityControllers: ConnectivityControllers? = null,
    private val hardwareObservationControllers: HardwareObservationControllers? = null,
    private val appLauncher: AppLauncher? = null,
    private val systemControlControllers: SystemControlControllers? = null,
    val commandRegistry: CommandRegistry = CommandRegistry()
) : GoalDispatcher {

    override fun dispatchGoal(goal: String): Boolean {
        return dispatchAndProcessWithLock(goal).result.status == SkillStatus.SUCCESS
    }

    fun dispatchAndProcessWithLock(goal: String): DispatchDetails {
        if (!executionController.acquireExecution()) {
            val rejectedRes = SkillResult(
                operation = "LOCK_REJECTED",
                status = SkillStatus.FAILED,
                message = "Goal dispatch rejected: another execution is active",
                durationMs = 0L,
                errorCode = "LOCK_REJECTED"
            )
            return DispatchDetails(goal, "LOCK_REJECTED", "ExecutionController", rejectedRes, "Execution Lock Rejected")
        }
        return try {
            val details = dispatchAndProcess(goal)
            executionController.releaseExecution(
                if (details.result.status == SkillStatus.SUCCESS) CancellationReason.NONE else CancellationReason.INTERNAL_FAILURE
            )
            details
        } catch (e: Exception) {
            executionController.releaseExecution(CancellationReason.INTERNAL_FAILURE)
            val errRes = SkillResult("ERROR", SkillStatus.FAILED, "Internal execution error: ${e.message}", 0L, "INTERNAL_ERROR")
            DispatchDetails(goal, "ERROR", "GoalDispatcherImpl", errRes, "Exception Thrown")
        }
    }

    override fun cancelCurrentGoal() {
        executionController.resetToSafeState(CancellationReason.USER_STOP)
    }

    fun dispatchAndProcess(goal: String): DispatchDetails {
        val trimmed = goal.trim()

        val cmdDef = commandRegistry.findCommandForInput(trimmed)
        if (cmdDef == null) {
            val res = SkillResult("UNKNOWN", SkillStatus.INVALID_GOAL, "Unrecognized command: '$trimmed'", 0L, "UNKNOWN_COMMAND")
            return DispatchDetails(trimmed, "UNKNOWN", "GoalDispatcherImpl", res, "Unrecognized Command")
        }

        if (cmdDef.status != CommandStatus.IMPLEMENTED) {
            val res = SkillResult(cmdDef.commandId, SkillStatus.UNAVAILABLE, "Command '${cmdDef.commandId}' is not implemented (${cmdDef.status})", 0L, "UNIMPLEMENTED_COMMAND")
            return DispatchDetails(trimmed, cmdDef.commandId, cmdDef.handlerIdentifier, res, "Command Unimplemented")
        }

        val parsedArgs = commandRegistry.parseArguments(trimmed, cmdDef)

        val opName = when (cmdDef.commandId) {
            "calculator.calculate" -> "CALCULATE"
            "notes.append" -> "NOTE"
            "timer.create" -> "SET_TIMER"
            "alarm.create" -> "SET_ALARM"
            "web.search" -> "WEB_SEARCH"
            "app.launch" -> "OPEN_APP"
            "flashlight.status", "flashlight.on", "flashlight.off" -> "FLASHLIGHT"
            "haptics.status", "haptics.vibrate" -> "VIBRATE"
            "brightness.status", "brightness.set" -> "BRIGHTNESS"
            "ringer.status", "ringer.normal", "ringer.vibrate", "ringer.silent" -> "RINGER"
            "diagnostics.status" -> "DEVICE_INFO"
            "sensor.list" -> "SENSOR_LIST"
            else -> cmdDef.commandId
        }

        return when (cmdDef.commandId) {
            "calculator.calculate" -> {
                val expr = parsedArgs.getString("expression") ?: ""
                val res = calculatorSkill.calculate(expr)
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, "Result = ${res.message}")
            }
            "notes.append" -> {
                val content = parsedArgs.getString("text") ?: ""
                val res = notesSkill.addNote(content)
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, "Persisted to file")
            }
            "timer.create" -> {
                val sec = parsedArgs.getInt("seconds", -1)
                val res = intentSkills?.setTimer(sec) ?: SkillResult("SET_TIMER", SkillStatus.UNAVAILABLE, "No Activity context", 0L, "NO_CONTEXT")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "alarm.create" -> {
                val timeStr = parsedArgs.getString("time") ?: ""
                val parts = timeStr.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: -1
                val m = parts.getOrNull(1)?.toIntOrNull() ?: -1
                val res = intentSkills?.setAlarm(h, m) ?: SkillResult("SET_ALARM", SkillStatus.UNAVAILABLE, "No Activity context", 0L, "NO_CONTEXT")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "web.search" -> {
                val q = parsedArgs.getString("query") ?: ""
                val res = intentSkills?.webSearch(q) ?: SkillResult("WEB_SEARCH", SkillStatus.UNAVAILABLE, "No Activity context", 0L, "NO_CONTEXT")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "app.launch" -> {
                val q = parsedArgs.getString("query") ?: ""
                if (appLauncher != null && appLauncher.findApp(q).isNotEmpty()) {
                    val launchRes = appLauncher.launchApp(q)
                    val status = if (launchRes.status == AppLaunchStatus.SUCCESS) SkillStatus.SUCCESS else SkillStatus.FAILED
                    val skillRes = SkillResult("LAUNCH_APP", status, launchRes.message, launchRes.durationMs, launchRes.errorCode)
                    DispatchDetails(trimmed, opName, "AppLauncherImpl", skillRes, "Package: ${launchRes.resolvedPackage ?: "NONE"}")
                } else if (q.contains("settings", ignoreCase = true)) {
                    val res = systemControlControllers?.openSystemSettings(q) ?: SkillResult("SYSTEM_SETTINGS", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                    DispatchDetails(trimmed, "SYSTEM_SETTINGS", "SystemControlControllers", res, res.message)
                } else if (appLauncher != null) {
                    val launchRes = appLauncher.launchApp(q)
                    val status = if (launchRes.status == AppLaunchStatus.SUCCESS) SkillStatus.SUCCESS else SkillStatus.FAILED
                    val skillRes = SkillResult("LAUNCH_APP", status, launchRes.message, launchRes.durationMs, launchRes.errorCode)
                    DispatchDetails(trimmed, opName, "AppLauncherImpl", skillRes, "Package: ${launchRes.resolvedPackage ?: "NONE"}")
                } else {
                    val errRes = SkillResult("LAUNCH_APP", SkillStatus.UNAVAILABLE, "AppLauncher unavailable", 0L, "NO_LAUNCHER")
                    DispatchDetails(trimmed, opName, "AppLauncherImpl", errRes, "No Launcher")
                }
            }
            "flashlight.status" -> {
                val res = flashlightController?.setFlashlight(false) ?: SkillResult("FLASHLIGHT", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "flashlight.on" -> {
                val res = flashlightController?.setFlashlight(true) ?: SkillResult("FLASHLIGHT", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, "Torch state = ON")
            }
            "flashlight.off" -> {
                val res = flashlightController?.setFlashlight(false) ?: SkillResult("FLASHLIGHT", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, "Torch state = OFF")
            }
            "haptics.status" -> {
                val res = hapticController?.vibrate(0L) ?: SkillResult("VIBRATE", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "haptics.vibrate" -> {
                val ms = parsedArgs.getLong("durationMs", -1L)
                val res = hapticController?.vibrate(ms) ?: SkillResult("VIBRATE", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "volume.music.status", "volume.ring.status", "volume.notification.status", "volume.alarm.status" -> {
                val streamName = cmdDef.commandId.split(".")[1]
                val streamType = volumeController?.parseStreamType(streamName) ?: android.media.AudioManager.STREAM_MUSIC
                val res = volumeController?.getVolumeStatus(streamType) ?: SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "VOLUME", cmdDef.handlerIdentifier, res, res.message)
            }
            "volume.music.current", "volume.ring.current", "volume.notification.current", "volume.alarm.current" -> {
                val streamName = cmdDef.commandId.split(".")[1]
                val streamType = volumeController?.parseStreamType(streamName) ?: android.media.AudioManager.STREAM_MUSIC
                val res = volumeController?.getCurrentIndex(streamType) ?: SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "VOLUME", cmdDef.handlerIdentifier, res, res.message)
            }
            "volume.music.maximum", "volume.ring.maximum", "volume.notification.maximum", "volume.alarm.maximum" -> {
                val streamName = cmdDef.commandId.split(".")[1]
                val streamType = volumeController?.parseStreamType(streamName) ?: android.media.AudioManager.STREAM_MUSIC
                val res = volumeController?.getMaximumIndex(streamType) ?: SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "VOLUME", cmdDef.handlerIdentifier, res, res.message)
            }
            "volume.music.percentage", "volume.ring.percentage", "volume.notification.percentage", "volume.alarm.percentage" -> {
                val streamName = cmdDef.commandId.split(".")[1]
                val streamType = volumeController?.parseStreamType(streamName) ?: android.media.AudioManager.STREAM_MUSIC
                val res = volumeController?.getPercentage(streamType) ?: SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "VOLUME", cmdDef.handlerIdentifier, res, res.message)
            }
            "volume.music.set", "volume.ring.set", "volume.notification.set", "volume.alarm.set" -> {
                val streamName = cmdDef.commandId.split(".")[1]
                val streamType = volumeController?.parseStreamType(streamName) ?: android.media.AudioManager.STREAM_MUSIC
                val percentStr = parsedArgs.getString("percentage")
                val percent = percentStr?.toIntOrNull() ?: -1
                val res = volumeController?.setVolumePercentage(percent, streamType) ?: SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "VOLUME", cmdDef.handlerIdentifier, res, res.message)
            }
            "wifi.status" -> {
                val res = connectivityControllers?.getWifiStatus() ?: SkillResult("WIFI", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "WIFI", cmdDef.handlerIdentifier, res, res.message)
            }
            "wifi.on" -> {
                val res = connectivityControllers?.setWifi(true) ?: SkillResult("WIFI", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "WIFI", cmdDef.handlerIdentifier, res, res.message)
            }
            "wifi.off" -> {
                val res = connectivityControllers?.setWifi(false) ?: SkillResult("WIFI", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "WIFI", cmdDef.handlerIdentifier, res, res.message)
            }
            "bluetooth.status" -> {
                val res = connectivityControllers?.getBluetoothStatus() ?: SkillResult("BLUETOOTH", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "BLUETOOTH", cmdDef.handlerIdentifier, res, res.message)
            }
            "bluetooth.on" -> {
                val res = connectivityControllers?.setBluetooth(true) ?: SkillResult("BLUETOOTH", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "BLUETOOTH", cmdDef.handlerIdentifier, res, res.message)
            }
            "bluetooth.off" -> {
                val res = connectivityControllers?.setBluetooth(false) ?: SkillResult("BLUETOOTH", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "BLUETOOTH", cmdDef.handlerIdentifier, res, res.message)
            }
            "battery.status" -> {
                val res = hardwareObservationControllers?.getBatteryStatus() ?: SkillResult("BATTERY", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "BATTERY", cmdDef.handlerIdentifier, res, res.message)
            }
            "sensor.list" -> {
                val res = hardwareObservationControllers?.getSensorList() ?: SkillResult("SENSOR_LIST", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "SENSOR_LIST", cmdDef.handlerIdentifier, res, res.message)
            }
            "sensor.accelerometer.sample" -> {
                val res = hardwareObservationControllers?.sampleSensor(android.hardware.Sensor.TYPE_ACCELEROMETER, "accelerometer")
                    ?: SkillResult("SENSOR", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "SENSOR", cmdDef.handlerIdentifier, res, res.message)
            }
            "sensor.gyroscope.sample" -> {
                val res = hardwareObservationControllers?.sampleSensor(android.hardware.Sensor.TYPE_GYROSCOPE, "gyroscope")
                    ?: SkillResult("SENSOR", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "SENSOR", cmdDef.handlerIdentifier, res, res.message)
            }
            "sensor.proximity.sample" -> {
                val res = hardwareObservationControllers?.sampleSensor(android.hardware.Sensor.TYPE_PROXIMITY, "proximity")
                    ?: SkillResult("SENSOR", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "SENSOR", cmdDef.handlerIdentifier, res, res.message)
            }
            "sensor.light.sample" -> {
                val res = hardwareObservationControllers?.sampleSensor(android.hardware.Sensor.TYPE_LIGHT, "light")
                    ?: SkillResult("SENSOR", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "SENSOR", cmdDef.handlerIdentifier, res, res.message)
            }
            "brightness.status" -> {
                val res = systemControlControllers?.getBrightnessStatus() ?: SkillResult("BRIGHTNESS", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "brightness.set" -> {
                val value = parsedArgs.getInt("value", -1)
                val res = systemControlControllers?.setBrightness(value) ?: SkillResult("BRIGHTNESS", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "ringer.status" -> {
                val res = systemControlControllers?.getRingerStatus() ?: SkillResult("RINGER", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "ringer.normal" -> {
                val res = systemControlControllers?.setRingerMode("normal") ?: SkillResult("RINGER", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "ringer.vibrate" -> {
                val res = systemControlControllers?.setRingerMode("vibrate") ?: SkillResult("RINGER", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "ringer.silent" -> {
                val res = systemControlControllers?.setRingerMode("silent") ?: SkillResult("RINGER", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "location.status" -> {
                val res = systemControlControllers?.getLocationStatus() ?: SkillResult("LOCATION_STATUS", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "LOCATION_STATUS", cmdDef.handlerIdentifier, res, res.message)
            }
            else -> {
                val res = SkillResult(cmdDef.commandId, SkillStatus.SUCCESS, "Executed command '${cmdDef.commandId}'", 0L)
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
        }
    }
}
