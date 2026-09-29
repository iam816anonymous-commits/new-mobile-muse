package com.agent.android.execution

import com.agent.android.agent.device.ConnectivityControllers
import com.agent.android.agent.device.FlashlightController
import com.agent.android.agent.device.HapticController
import com.agent.android.agent.device.HardwareObservationControllers
import com.agent.android.agent.device.VolumeController
import com.agent.android.agent.skills.CalculatorSkill
import com.agent.android.agent.skills.IntentSkills
import com.agent.android.agent.skills.NotesSkill
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus
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
    private val hardwareObservationControllers: HardwareObservationControllers? = null
) : GoalDispatcher {

    override fun dispatchGoal(goal: String): Boolean {
        if (!executionController.acquireExecution()) {
            return false
        }
        return try {
            val details = dispatchAndProcess(goal)
            executionController.releaseExecution(
                if (details.result.status == SkillStatus.SUCCESS) CancellationReason.NONE else CancellationReason.INTERNAL_FAILURE
            )
            details.result.status == SkillStatus.SUCCESS
        } catch (e: Exception) {
            executionController.releaseExecution(CancellationReason.INTERNAL_FAILURE)
            false
        }
    }

    override fun cancelCurrentGoal() {
        executionController.resetToSafeState(CancellationReason.USER_STOP)
    }

    fun dispatchAndProcess(goal: String): DispatchDetails {
        val trimmed = goal.trim()
        val lower = trimmed.lowercase()

        return when {
            lower.startsWith("calculate") -> {
                val expr = trimmed.substringAfter("calculate").trim()
                val res = calculatorSkill.calculate(expr)
                DispatchDetails(trimmed, "CALCULATE", "CalculatorSkill", res, "Result = ${res.message}")
            }
            lower.startsWith("note down") -> {
                val content = trimmed.substringAfter("note down").trim()
                val res = notesSkill.addNote(content)
                DispatchDetails(trimmed, "NOTE", "NotesSkill", res, "Persisted to file")
            }
            lower.startsWith("timer") -> {
                val minStr = trimmed.substringAfter("timer").replace("[^0-9]".toRegex(), "")
                val min = minStr.toIntOrNull() ?: 0
                val res = intentSkills?.setTimer(min) ?: SkillResult("SET_TIMER", SkillStatus.UNAVAILABLE, "No Activity context", 0L)
                DispatchDetails(trimmed, "SET_TIMER", "IntentSkills", res, "AlarmClock Intent Launched")
            }
            lower.startsWith("alarm") || lower.startsWith("set alarm") -> {
                val timeStr = trimmed.substringAfter("alarm").trim()
                val parts = timeStr.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: -1
                val m = parts.getOrNull(1)?.toIntOrNull() ?: -1
                val res = intentSkills?.setAlarm(h, m) ?: SkillResult("SET_ALARM", SkillStatus.UNAVAILABLE, "No Activity context", 0L)
                DispatchDetails(trimmed, "SET_ALARM", "IntentSkills", res, "AlarmClock Intent Launched")
            }
            lower.startsWith("search") || lower.startsWith("web search") -> {
                val query = if (lower.startsWith("search")) trimmed.substringAfter("search").trim() else trimmed.substringAfter("web search").trim()
                val res = intentSkills?.webSearch(query) ?: SkillResult("WEB_SEARCH", SkillStatus.UNAVAILABLE, "No Activity context", 0L)
                DispatchDetails(trimmed, "WEB_SEARCH", "IntentSkills", res, "WebSearch Intent Launched")
            }
            lower.startsWith("flashlight") -> {
                val enable = lower.endsWith("on")
                val res = flashlightController?.setFlashlight(enable) ?: SkillResult("FLASHLIGHT", SkillStatus.UNAVAILABLE, "No Controller", 0L)
                DispatchDetails(trimmed, "FLASHLIGHT", "FlashlightController", res, "Torch state = ${if (enable) "ON" else "OFF"}")
            }
            lower.startsWith("vibrate") -> {
                val ms = trimmed.substringAfter("vibrate").replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 300L
                val res = hapticController?.vibrate(ms) ?: SkillResult("VIBRATE", SkillStatus.UNAVAILABLE, "No Controller", 0L)
                DispatchDetails(trimmed, "VIBRATE", "HapticController", res, "Haptics triggered")
            }
            lower.startsWith("volume") -> {
                val res = volumeController?.getVolume() ?: SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "No Controller", 0L)
                DispatchDetails(trimmed, "VOLUME", "VolumeController", res, res.message)
            }
            lower.startsWith("wifi") -> {
                val res = if (lower.contains("on")) {
                    connectivityControllers?.setWifi(true) ?: SkillResult("WIFI", SkillStatus.UNAVAILABLE, "No Controller", 0L)
                } else if (lower.contains("off")) {
                    connectivityControllers?.setWifi(false) ?: SkillResult("WIFI", SkillStatus.UNAVAILABLE, "No Controller", 0L)
                } else {
                    connectivityControllers?.getWifiStatus() ?: SkillResult("WIFI", SkillStatus.UNAVAILABLE, "No Controller", 0L)
                }
                DispatchDetails(trimmed, "WIFI", "ConnectivityControllers", res, res.message)
            }
            lower.startsWith("bluetooth") -> {
                val res = connectivityControllers?.getBluetoothStatus() ?: SkillResult("BLUETOOTH", SkillStatus.UNAVAILABLE, "No Controller", 0L)
                DispatchDetails(trimmed, "BLUETOOTH", "ConnectivityControllers", res, res.message)
            }
            lower.startsWith("battery") -> {
                val res = hardwareObservationControllers?.getBatteryStatus() ?: SkillResult("BATTERY", SkillStatus.UNAVAILABLE, "No Controller", 0L)
                DispatchDetails(trimmed, "BATTERY", "HardwareObservationControllers", res, res.message)
            }
            lower.startsWith("sensor") -> {
                val sensorType = when {
                    lower.contains("accel") -> android.hardware.Sensor.TYPE_ACCELEROMETER
                    lower.contains("gyro") -> android.hardware.Sensor.TYPE_GYROSCOPE
                    lower.contains("prox") -> android.hardware.Sensor.TYPE_PROXIMITY
                    lower.contains("light") -> android.hardware.Sensor.TYPE_LIGHT
                    else -> -1
                }
                val res = if (sensorType != -1) {
                    hardwareObservationControllers?.sampleSensor(sensorType, trimmed.substringAfter("sensor").trim())
                        ?: SkillResult("SENSOR", SkillStatus.UNAVAILABLE, "No Controller", 0L)
                } else {
                    SkillResult("SENSOR", SkillStatus.FAILED, "Nonexistent sensor type", 0L, "NO_SENSOR")
                }
                DispatchDetails(trimmed, "SENSOR", "HardwareObservationControllers", res, res.message)
            }
            else -> {
                val res = SkillResult("UNKNOWN", SkillStatus.INVALID_GOAL, "Unrecognized deterministic goal", 0L, "UNKNOWN_COMMAND")
                DispatchDetails(trimmed, "UNKNOWN", "GoalDispatcherImpl", res, "Invalid goal command")
            }
        }
    }
}
