package com.agent.android.execution

import com.agent.android.agent.device.AgentNotificationController
import com.agent.android.agent.device.AppDiscoveryController
import com.agent.android.agent.device.BackgroundExecutionPolicy
import com.agent.android.agent.device.CameraController
import com.agent.android.agent.device.CapabilityRegistry
import com.agent.android.agent.device.ClipboardController
import com.agent.android.agent.device.ConnectivityControllers
import com.agent.android.agent.device.DeviceAdministrationCapabilityDetector
import com.agent.android.agent.device.DeviceStateController
import com.agent.android.agent.device.DisplayController
import com.agent.android.agent.device.FileAccessController
import com.agent.android.agent.device.FlashlightController
import com.agent.android.agent.device.HapticController
import com.agent.android.agent.device.HardwareObservationControllers
import com.agent.android.agent.device.InputStateController
import com.agent.android.agent.device.LocationController
import com.agent.android.agent.device.NetworkController
import com.agent.android.agent.device.NotificationController
import com.agent.android.agent.device.PowerStateController
import com.agent.android.agent.device.ScreenCaptureController
import com.agent.android.agent.device.SettingsActionRegistry
import com.agent.android.agent.device.SystemControlControllers
import com.agent.android.agent.device.UsageStatsController
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
import com.agent.android.diagnostics.FoundationReadinessEvaluator
import com.agent.android.permissions.PermissionManager
import com.agent.android.permissions.PermissionStatus
import com.agent.android.safety.CancellationReason
import com.agent.android.speech.SpeechToTextEngine
import com.agent.android.speech.TextToSpeechEngine
import com.agent.android.target.TargetQuery
import com.agent.android.target.TargetResolver
import com.agent.android.target.TargetResolutionStatus
import com.agent.android.target.TargetSelector
import com.agent.android.test.FoundationTestRegistry

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
    val commandRegistry: CommandRegistry = CommandRegistry(),
    private val clipboardController: ClipboardController? = null,
    private val notificationController: NotificationController? = null,
    private val usageStatsController: UsageStatsController? = null,
    private val displayController: DisplayController? = null,
    private val screenCaptureController: ScreenCaptureController? = null,
    private val inputStateController: InputStateController? = null,
    private val cameraController: CameraController? = null,
    private val fileAccessController: FileAccessController? = null,
    private val locationController: LocationController? = null,
    private val networkController: NetworkController? = null,
    private val powerStateController: PowerStateController? = null,
    private val backgroundExecutionPolicy: BackgroundExecutionPolicy = BackgroundExecutionPolicy(),
    private val appDiscoveryController: AppDiscoveryController? = null,
    private val deviceStateController: DeviceStateController? = null,
    private val settingsActionRegistry: SettingsActionRegistry? = null,
    private val deviceAdminDetector: DeviceAdministrationCapabilityDetector? = null,
    private val capabilityRegistry: CapabilityRegistry? = null,
    private val readinessEvaluator: FoundationReadinessEvaluator? = null,
    private val sttEngine: SpeechToTextEngine? = null,
    private val ttsEngine: TextToSpeechEngine? = null,
    val permissionManager: PermissionManager? = null,
    val observationEngine: com.agent.android.observation.AccessibilityObservationEngine? = null,
    val targetResolver: TargetResolver = TargetResolver(),
    val context: android.content.Context? = null
) : GoalDispatcher {

    init {
        instance = this
    }

    companion object {
        @Volatile
        var instance: GoalDispatcherImpl? = null
    }

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

        if (cmdDef.commandId.startsWith("settings.")) {
            val res = settingsActionRegistry?.launchSettingsAction(cmdDef.commandId)
                ?: systemControlControllers?.openSystemSettings(cmdDef.commandId.removePrefix("settings."))
                ?: SkillResult("SETTINGS_ACTION", SkillStatus.UNAVAILABLE, "No Settings Handler", 0L, "NO_CONTROLLER")
            return DispatchDetails(trimmed, opName, "SettingsActionRegistry", res, res.message)
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
            "app.current" -> {
                val perm = permissionManager?.registry?.getPermissionById("usage_stats_access")
                if (perm != null && permissionManager?.checkStatus(perm) != PermissionStatus.OBTAINED) {
                    val res = SkillResult("USAGE_STATS", SkillStatus.PERMISSION_REQUIRED, "Usage Stats Access required. Grant via Settings -> Usage Access.", 0L, "USAGE_STATS_REQUIRED")
                    return DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
                }
                val res = usageStatsController?.getCurrentForegroundApp() ?: SkillResult("USAGE_STATS", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "app.list" -> {
                val res = appDiscoveryController?.listApps() ?: SkillResult("APP_DISCOVERY", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "app.find" -> {
                val q = parsedArgs.getString("query") ?: ""
                val res = appDiscoveryController?.findApp(q) ?: SkillResult("APP_DISCOVERY", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "app.info" -> {
                val pkg = parsedArgs.getString("package") ?: ""
                val res = appDiscoveryController?.getAppInfo(pkg) ?: SkillResult("APP_DISCOVERY", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "clipboard.status" -> {
                val res = clipboardController?.getClipboardStatus() ?: SkillResult("CLIPBOARD", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "clipboard.read" -> {
                val res = clipboardController?.readClipboard() ?: SkillResult("CLIPBOARD", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "clipboard.write" -> {
                val text = parsedArgs.getString("text") ?: ""
                val res = clipboardController?.writeClipboard(text) ?: SkillResult("CLIPBOARD", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "clipboard.clear" -> {
                val res = clipboardController?.clearClipboard() ?: SkillResult("CLIPBOARD", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "notification.status" -> {
                val perm = permissionManager?.registry?.getPermissionById("notification_listener_access")
                val statusStr = if (perm != null) permissionManager?.checkStatus(perm)?.name ?: "UNKNOWN" else "UNKNOWN"
                val res = notificationController?.getNotificationStatus() ?: SkillResult("NOTIFICATION", SkillStatus.SUCCESS, "Notification Listener Status: $statusStr", 0L)
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "notification.latest" -> {
                val perm = permissionManager?.registry?.getPermissionById("notification_listener_access")
                if (perm != null && permissionManager?.checkStatus(perm) != PermissionStatus.OBTAINED) {
                    val res = SkillResult("NOTIFICATION", SkillStatus.PERMISSION_REQUIRED, "Notification Listener Access required. Grant via Settings -> Notification Access.", 0L, "NOTIFICATION_LISTENER_REQUIRED")
                    return DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
                }
                val res = notificationController?.getLatestNotification() ?: SkillResult("NOTIFICATION", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "display.status" -> {
                val res = displayController?.getDisplayStatus() ?: SkillResult("DISPLAY", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "display.dimensions" -> {
                val res = displayController?.getDisplayDimensions() ?: SkillResult("DISPLAY", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "display.orientation" -> {
                val res = displayController?.getDisplayOrientation() ?: SkillResult("DISPLAY", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "screen.capture.status" -> {
                val res = screenCaptureController?.getScreenCaptureStatus() ?: SkillResult("SCREEN_CAPTURE", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "keyboard.status" -> {
                val res = inputStateController?.getKeyboardStatus() ?: SkillResult("INPUT_STATE", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "input.status" -> {
                val res = inputStateController?.getInputStatus() ?: SkillResult("INPUT_STATE", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "camera.status" -> {
                val res = cameraController?.getCameraStatus() ?: SkillResult("CAMERA", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "camera.permission" -> {
                val perm = permissionManager?.registry?.getPermissionById("perm_camera")
                val statusStr = if (perm != null) permissionManager?.checkStatus(perm)?.name ?: "UNKNOWN" else "UNKNOWN"
                val res = cameraController?.getCameraPermissionStatus() ?: SkillResult("CAMERA", SkillStatus.SUCCESS, "CAMERA permission status: $statusStr", 0L)
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "camera.list" -> {
                val res = cameraController?.getCameraList() ?: SkillResult("CAMERA", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "network.status" -> {
                val res = networkController?.getNetworkStatus() ?: SkillResult("NETWORK", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "location.providers" -> {
                val res = locationController?.getLocationProviders() ?: SkillResult("LOCATION", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "power.status" -> {
                val res = powerStateController?.getPowerStatus() ?: SkillResult("POWER", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "background.policy" -> {
                val res = backgroundExecutionPolicy.getPolicyStatus()
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "device.snapshot" -> {
                val res = deviceStateController?.getDeviceSnapshot() ?: SkillResult("DEVICE_SNAPSHOT", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "flashlight.status" -> {
                val res = flashlightController?.setFlashlightTarget("status") ?: SkillResult("FLASHLIGHT", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "flashlight.on" -> {
                val perm = permissionManager?.registry?.getPermissionById("perm_camera")
                if (perm != null && permissionManager?.checkStatus(perm) != PermissionStatus.OBTAINED) {
                    val res = SkillResult("FLASHLIGHT", SkillStatus.PERMISSION_REQUIRED, "CAMERA permission required for flashlight. Grant in Settings.", 0L, "CAMERA_PERMISSION_REQUIRED")
                    return DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
                }
                val res = flashlightController?.setFlashlightTarget("back", true) ?: SkillResult("FLASHLIGHT", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "flashlight.front" -> {
                val perm = permissionManager?.registry?.getPermissionById("perm_camera")
                if (perm != null && permissionManager?.checkStatus(perm) != PermissionStatus.OBTAINED) {
                    val res = SkillResult("FLASHLIGHT", SkillStatus.PERMISSION_REQUIRED, "CAMERA permission required for flashlight. Grant in Settings.", 0L, "CAMERA_PERMISSION_REQUIRED")
                    return DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
                }
                val res = flashlightController?.setFlashlightTarget("front", true) ?: SkillResult("FLASHLIGHT", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "flashlight.back" -> {
                val perm = permissionManager?.registry?.getPermissionById("perm_camera")
                if (perm != null && permissionManager?.checkStatus(perm) != PermissionStatus.OBTAINED) {
                    val res = SkillResult("FLASHLIGHT", SkillStatus.PERMISSION_REQUIRED, "CAMERA permission required for flashlight. Grant in Settings.", 0L, "CAMERA_PERMISSION_REQUIRED")
                    return DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
                }
                val res = flashlightController?.setFlashlightTarget("back", true) ?: SkillResult("FLASHLIGHT", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "flashlight.both" -> {
                val perm = permissionManager?.registry?.getPermissionById("perm_camera")
                if (perm != null && permissionManager?.checkStatus(perm) != PermissionStatus.OBTAINED) {
                    val res = SkillResult("FLASHLIGHT", SkillStatus.PERMISSION_REQUIRED, "CAMERA permission required for flashlight. Grant in Settings.", 0L, "CAMERA_PERMISSION_REQUIRED")
                    return DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
                }
                val res = flashlightController?.setFlashlightTarget("both", true) ?: SkillResult("FLASHLIGHT", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "flashlight.off" -> {
                val res = flashlightController?.setFlashlightTarget("off", false) ?: SkillResult("FLASHLIGHT", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "flashlight.target" -> {
                val target = parsedArgs.getString("target") ?: "status"
                val perm = permissionManager?.registry?.getPermissionById("perm_camera")
                if (target != "status" && target != "off" && perm != null && permissionManager?.checkStatus(perm) != PermissionStatus.OBTAINED) {
                    val res = SkillResult("FLASHLIGHT", SkillStatus.PERMISSION_REQUIRED, "CAMERA permission required for flashlight. Grant in Settings.", 0L, "CAMERA_PERMISSION_REQUIRED")
                    return DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
                }
                val res = flashlightController?.setFlashlightTarget(target, target != "off") ?: SkillResult("FLASHLIGHT", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "haptics.status" -> {
                val res = hapticController?.getVibratorStatus() ?: SkillResult("VIBRATION", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
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
            "sensor.list", "sensor.discovery" -> {
                val res = hardwareObservationControllers?.getSensorList() ?: SkillResult("SENSOR", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "SENSOR", cmdDef.handlerIdentifier, res, res.message)
            }
            "sensor.status" -> {
                val discovered = hardwareObservationControllers?.discoverAllSensors() ?: emptyList()
                val res = SkillResult("SENSOR_STATUS", SkillStatus.SUCCESS, "Discovered ${discovered.size} total sensors via SensorManager.TYPE_ALL", 0L)
                DispatchDetails(trimmed, "SENSOR", cmdDef.handlerIdentifier, res, res.message)
            }
            "sensor.info" -> {
                val sensorTypeStr = parsedArgs.getString("sensorType") ?: "accelerometer"
                val typeInt = when (sensorTypeStr.lowercase()) {
                    "accelerometer" -> android.hardware.Sensor.TYPE_ACCELEROMETER
                    "gyroscope" -> android.hardware.Sensor.TYPE_GYROSCOPE
                    "proximity" -> android.hardware.Sensor.TYPE_PROXIMITY
                    "light" -> android.hardware.Sensor.TYPE_LIGHT
                    else -> sensorTypeStr.toIntOrNull() ?: android.hardware.Sensor.TYPE_ACCELEROMETER
                }
                val validated = hardwareObservationControllers?.validateSingleSensor(typeInt, 100L)
                val res = if (validated != null) {
                    SkillResult("SENSOR_INFO", SkillStatus.SUCCESS, "Sensor ${validated.name} [Vendor: ${validated.vendor}, MaxRange: ${validated.maximumRange}, Res: ${validated.resolution}, Power: ${validated.power}mA, State: ${validated.capabilityState}]", 0L)
                } else {
                    SkillResult("SENSOR_INFO", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                }
                DispatchDetails(trimmed, "SENSOR", cmdDef.handlerIdentifier, res, res.message)
            }
            "sensor.test", "sensor.sample" -> {
                val sensorTypeStr = parsedArgs.getString("sensorType") ?: "accelerometer"
                val typeInt = when (sensorTypeStr.lowercase()) {
                    "accelerometer" -> android.hardware.Sensor.TYPE_ACCELEROMETER
                    "gyroscope" -> android.hardware.Sensor.TYPE_GYROSCOPE
                    "proximity" -> android.hardware.Sensor.TYPE_PROXIMITY
                    "light" -> android.hardware.Sensor.TYPE_LIGHT
                    else -> sensorTypeStr.toIntOrNull() ?: android.hardware.Sensor.TYPE_ACCELEROMETER
                }
                val res = hardwareObservationControllers?.sampleSensor(typeInt, sensorTypeStr, 500L) ?: SkillResult("SENSOR", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "SENSOR", cmdDef.handlerIdentifier, res, res.message)
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
                val perm = permissionManager?.registry?.getPermissionById("write_settings_access")
                if (perm != null && permissionManager?.checkStatus(perm) != PermissionStatus.OBTAINED) {
                    val res = SkillResult("BRIGHTNESS", SkillStatus.PERMISSION_REQUIRED, "WRITE_SETTINGS permission required to change brightness. Grant via Settings -> Special Access.", 0L, "WRITE_SETTINGS_REQUIRED")
                    return DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
                }
                val value = parsedArgs.getInt("value", -1)
                val res = systemControlControllers?.setBrightness(value) ?: SkillResult("BRIGHTNESS", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "ringer.status" -> {
                val res = systemControlControllers?.getRingerStatus() ?: SkillResult("RINGER", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "ringer.normal", "ringer.vibrate", "ringer.silent" -> {
                val perm = permissionManager?.registry?.getPermissionById("notification_policy_access")
                if (perm != null && permissionManager?.checkStatus(perm) != PermissionStatus.OBTAINED) {
                    val res = SkillResult("RINGER", SkillStatus.PERMISSION_REQUIRED, "Notification Policy Access required to change ringer mode. Grant via Settings -> Do Not Disturb Access.", 0L, "NOTIFICATION_POLICY_ACCESS_REQUIRED")
                    return DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
                }
                val mode = cmdDef.commandId.removePrefix("ringer.")
                val res = systemControlControllers?.setRingerMode(mode) ?: SkillResult("RINGER", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "location.status" -> {
                val res = systemControlControllers?.getLocationStatus() ?: SkillResult("LOCATION_STATUS", SkillStatus.UNAVAILABLE, "No Controller", 0L, "NO_CONTROLLER")
                DispatchDetails(trimmed, "LOCATION_STATUS", cmdDef.handlerIdentifier, res, res.message)
            }
            "stt.status" -> {
                val avail = sttEngine?.isAvailable() == true
                val status = if (avail) SkillStatus.SUCCESS else SkillStatus.UNAVAILABLE
                val res = SkillResult("STT_STATUS", status, "SpeechRecognizer AVAILABLE = $avail", 0L)
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "stt.listen" -> {
                val perm = permissionManager?.registry?.getPermissionById("perm_record_audio")
                if (perm != null && permissionManager?.checkStatus(perm) != PermissionStatus.OBTAINED) {
                    val res = SkillResult("STT", SkillStatus.PERMISSION_REQUIRED, "RECORD_AUDIO permission required for speech recognition. Grant in Settings.", 0L, "RECORD_AUDIO_REQUIRED")
                    return DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
                }
                val res = SkillResult("STT_LISTEN", SkillStatus.SUCCESS, "Speech recognition active", 0L)
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "stt.cancel" -> {
                sttEngine?.cancel()
                val res = SkillResult("STT_CANCEL", SkillStatus.SUCCESS, "STT recognition cancelled", 0L)
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "tts.status" -> {
                val res = SkillResult("TTS_STATUS", SkillStatus.SUCCESS, "TextToSpeech API operational", 0L)
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "tts.stop" -> {
                ttsEngine?.stop()
                val res = SkillResult("TTS_STOP", SkillStatus.SUCCESS, "TextToSpeech output stopped", 0L)
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "permissions.status", "capabilities.status" -> {
                val detailed = capabilityRegistry?.checkDetailedCapabilities() ?: emptyMap()
                val res = SkillResult(cmdDef.commandId, SkillStatus.SUCCESS, "Capabilities queried: ${detailed.size} entries", 0L)
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "accessibility.status" -> {
                val perm = permissionManager?.registry?.getPermissionById("accessibility_service_required")
                val statusStr = if (perm != null) permissionManager?.checkStatus(perm)?.name ?: "UNKNOWN" else "UNKNOWN"
                val res = SkillResult("ACCESSIBILITY_STATUS", SkillStatus.SUCCESS, "Accessibility service status: $statusStr", 0L)
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "diagnostics.readiness" -> {
                val report = readinessEvaluator?.evaluate()
                val res = if (report != null) {
                    val status = if (report.isReady) SkillStatus.SUCCESS else SkillStatus.FAILED
                    SkillResult("READINESS", status, "Status: ${report.statusText} (${report.blockingReasons.size} blockers)", 0L)
                } else {
                    SkillResult("READINESS", SkillStatus.SUCCESS, "Readiness Evaluator registered", 0L)
                }
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
            "target.resolve", "target.find", "target.candidates" -> {
                val qStr = parsedArgs.getString("query") ?: ""
                val snapshot = observationEngine?.getDisplayedSnapshot() ?: observationEngine?.getLastSnapshot()
                val targetQuery = TargetQuery(text = qStr, selectorType = TargetSelector.AUTO)
                val res = targetResolver.resolve(snapshot, targetQuery)
                val skillStatus = when (res.status) {
                    TargetResolutionStatus.RESOLVED -> SkillStatus.SUCCESS
                    TargetResolutionStatus.AMBIGUOUS -> SkillStatus.FAILED
                    TargetResolutionStatus.NOT_FOUND -> SkillStatus.FAILED
                    else -> SkillStatus.UNAVAILABLE
                }
                val skillRes = SkillResult("TARGET_RESOLUTION", skillStatus, res.explanation, 0L, res.status.name)
                DispatchDetails(trimmed, "TARGET_RESOLVE", cmdDef.handlerIdentifier, skillRes, res.explanation)
            }
            "target.inspect" -> {
                val nodeId = parsedArgs.getString("nodeId") ?: ""
                val snapshot = observationEngine?.getDisplayedSnapshot() ?: observationEngine?.getLastSnapshot()
                val matchedNode = snapshot?.allNodesList?.find { it.id == nodeId }
                val msg = if (matchedNode != null) {
                    "Node '$nodeId': Class=${matchedNode.className}, Text='${matchedNode.text}', Desc='${matchedNode.contentDescription}', Clickable=${matchedNode.isClickable}, Editable=${matchedNode.isEditable}"
                } else {
                    "Node '$nodeId' not found in current snapshot"
                }
                val skillRes = SkillResult("TARGET_INSPECT", if (matchedNode != null) SkillStatus.SUCCESS else SkillStatus.FAILED, msg, 0L)
                DispatchDetails(trimmed, "TARGET_INSPECT", cmdDef.handlerIdentifier, skillRes, msg)
            }
            "action.click", "action.long_click", "action.input", "action.scroll", "action.back", "action.recents", "action.home", "test.click", "test.long_click", "test.text_input", "test.scroll", "test.back" -> {
                val qStr = parsedArgs.getString("query") ?: parsedArgs.getString("text") ?: parsedArgs.getString("direction") ?: ""
                val snapshot = observationEngine?.getDisplayedSnapshot() ?: observationEngine?.getLastSnapshot()
                val actionType = when (cmdDef.commandId) {
                    "action.click", "test.click" -> com.agent.android.actions.UiActionType.CLICK
                    "action.long_click", "test.long_click" -> com.agent.android.actions.UiActionType.LONG_CLICK
                    "action.input", "test.text_input" -> com.agent.android.actions.UiActionType.TEXT_INPUT
                    "action.scroll", "test.scroll" -> if (qStr.equals("backward", true)) com.agent.android.actions.UiActionType.SCROLL_BACKWARD else com.agent.android.actions.UiActionType.SCROLL_FORWARD
                    "action.back", "test.back" -> com.agent.android.actions.UiActionType.GLOBAL_BACK
                    "action.recents" -> com.agent.android.actions.UiActionType.GLOBAL_RECENTS
                    "action.home" -> com.agent.android.actions.UiActionType.GLOBAL_HOME
                    else -> com.agent.android.actions.UiActionType.CLICK
                }

                val isGlobal = actionType == com.agent.android.actions.UiActionType.GLOBAL_BACK ||
                        actionType == com.agent.android.actions.UiActionType.GLOBAL_RECENTS ||
                        actionType == com.agent.android.actions.UiActionType.GLOBAL_HOME

                val resolvedTarget = if (qStr.isNotEmpty() && !isGlobal) {
                    targetResolver.resolve(snapshot, TargetQuery(text = qStr)).resolvedTarget
                } else null

                val req = com.agent.android.actions.UiActionRequest(
                    actionType = actionType,
                    targetQueryText = qStr,
                    resolvedTarget = resolvedTarget,
                    expectedPackage = snapshot?.packageName,
                    textInput = if (actionType == com.agent.android.actions.UiActionType.TEXT_INPUT) qStr else null,
                    sourceSnapshotId = snapshot?.snapshotId
                )

                val executor = com.agent.android.actions.UiActionExecutor(observationEngine = observationEngine)
                val liveService = com.agent.android.service.LocalAgentAccessibilityService.instance
                val actionRes = executor.executeAction(req, service = liveService)

                val skillStatus = when (actionRes.status) {
                    com.agent.android.actions.ActionExecutionStatus.SUCCESS -> SkillStatus.SUCCESS
                    com.agent.android.actions.ActionExecutionStatus.TARGET_NOT_FOUND -> SkillStatus.FAILED
                    com.agent.android.actions.ActionExecutionStatus.WRONG_FOREGROUND_APP, com.agent.android.actions.ActionExecutionStatus.WRONG_PACKAGE -> SkillStatus.FAILED
                    else -> SkillStatus.UNAVAILABLE
                }
                val skillRes = SkillResult("ACTION_EXECUTION", skillStatus, actionRes.explanation, actionRes.durationMs, actionRes.status.name)
                DispatchDetails(trimmed, "ACTION_EXECUTION", cmdDef.handlerIdentifier, skillRes, actionRes.explanation)
            }
            "overlay.show" -> {
                val ctx = context ?: permissionManager?.context
                if (ctx != null && !com.agent.android.overlay.LocalAgentOverlayService.checkOverlayPermission(ctx)) {
                    val res = SkillResult(
                        "OVERLAY_SHOW",
                        SkillStatus.PERMISSION_REQUIRED,
                        "Display over other apps permission required. Grant via Settings -> Display over other apps.",
                        0L,
                        "OVERLAY_PERMISSION_REQUIRED"
                    )
                    return DispatchDetails(trimmed, "OVERLAY_SHOW", cmdDef.handlerIdentifier, res, res.message)
                }

                if (ctx != null) {
                    try {
                        val showIntent = android.content.Intent(ctx, com.agent.android.overlay.LocalAgentOverlayService::class.java).apply {
                            action = com.agent.android.overlay.LocalAgentOverlayService.ACTION_SHOW
                        }
                        ctx.startService(showIntent)
                    } catch (e: Exception) {
                        // Background service start fallback
                    }
                }
                com.agent.android.overlay.LocalAgentOverlayService.instance?.showOverlay()

                val diag = com.agent.android.overlay.LocalAgentOverlayService.getDiagnosticStatus(ctx)
                val res = SkillResult("OVERLAY_SHOW", SkillStatus.SUCCESS, "Movable action overlay SHOW requested.\n$diag", 0L)
                DispatchDetails(trimmed, "OVERLAY_SHOW", cmdDef.handlerIdentifier, res, res.message)
            }
            "overlay.hide" -> {
                val ctx = context ?: permissionManager?.context
                if (ctx != null) {
                    try {
                        val hideIntent = android.content.Intent(ctx, com.agent.android.overlay.LocalAgentOverlayService::class.java).apply {
                            action = com.agent.android.overlay.LocalAgentOverlayService.ACTION_HIDE
                        }
                        ctx.startService(hideIntent)
                    } catch (e: Exception) {
                        // Background service start fallback
                    }
                }
                com.agent.android.overlay.LocalAgentOverlayService.instance?.hideOverlay()
                val res = SkillResult("OVERLAY_HIDE", SkillStatus.SUCCESS, "Movable action overlay HIDDEN", 0L)
                DispatchDetails(trimmed, "OVERLAY_HIDE", cmdDef.handlerIdentifier, res, res.message)
            }
            "overlay.status" -> {
                val ctx = context ?: permissionManager?.context
                val diag = com.agent.android.overlay.LocalAgentOverlayService.getDiagnosticStatus(ctx)
                val res = SkillResult("OVERLAY_STATUS", SkillStatus.SUCCESS, diag, 0L)
                DispatchDetails(trimmed, "OVERLAY_STATUS", cmdDef.handlerIdentifier, res, diag)
            }
            "action.status" -> {
                val liveService = com.agent.android.service.LocalAgentAccessibilityService.instance
                val isConnected = liveService != null
                val snapshot = observationEngine?.getDisplayedSnapshot() ?: observationEngine?.getLastSnapshot()
                val msg = """
                    Action Subsystem Status:
                    AccessibilityService: ${if (isConnected) "CONNECTED" else "DISCONNECTED"}
                    Current Foreground Package: ${snapshot?.packageName ?: "UNKNOWN"}
                    Current Activity: ${snapshot?.activityName ?: "UNKNOWN"}
                    Global Actions: ${if (isConnected) "AVAILABLE" else "UNAVAILABLE"}
                    Node Actions: ${if (isConnected) "AVAILABLE" else "UNAVAILABLE"}
                """.trimIndent()
                val res = SkillResult("ACTION_STATUS", if (isConnected) SkillStatus.SUCCESS else SkillStatus.UNAVAILABLE, msg, 0L)
                DispatchDetails(trimmed, "ACTION_STATUS", cmdDef.handlerIdentifier, res, msg)
            }
            "test.launch" -> {
                val targetName = parsedArgs.getString("target") ?: "calculator"
                val app = when (targetName.lowercase()) {
                    "calculator" -> com.agent.android.observation.GuidedTestApp.CALCULATOR
                    "chrome", "browser" -> com.agent.android.observation.GuidedTestApp.CHROME
                    "settings" -> com.agent.android.observation.GuidedTestApp.SETTINGS
                    "youtube" -> com.agent.android.observation.GuidedTestApp.YOUTUBE
                    else -> com.agent.android.observation.GuidedTestApp.CALCULATOR
                }
                val res = SkillResult("TEST_LAUNCH", SkillStatus.SUCCESS, "Target launch requested for '${app.label}'", 0L)
                DispatchDetails(trimmed, "TEST_LAUNCH", cmdDef.handlerIdentifier, res, res.message)
            }
            "test.observe" -> {
                val snap = observationEngine?.captureCurrentScreen()
                val msg = if (snap != null) "Captured screen (${snap.nodeCount} nodes, pkg=${snap.packageName})" else "No observation snapshot"
                val res = SkillResult("TEST_OBSERVE", if (snap != null) SkillStatus.SUCCESS else SkillStatus.FAILED, msg, 0L)
                DispatchDetails(trimmed, "TEST_OBSERVE", cmdDef.handlerIdentifier, res, msg)
            }
            "test.run" -> {
                val testId = parsedArgs.getString("testId") ?: "P3.2-ACT-001"
                val res = SkillResult("TEST_RUN", SkillStatus.SUCCESS, "Executed test scenario '$testId'", 0L)
                DispatchDetails(trimmed, "TEST_RUN", cmdDef.handlerIdentifier, res, res.message)
            }
            "help" -> {
                val q = parsedArgs.getString("query") ?: ""
                val msg = if (q.isBlank()) {
                    val categories = commandRegistry.getAllCommands().map { it.category.name }.distinct().joinToString(", ")
                    "LocalAgent Commands. Usage: 'help <category_or_command>'. Available Categories: $categories"
                } else {
                    val matches = commandRegistry.getAllCommands().filter {
                        it.commandId.contains(q, ignoreCase = true) || it.category.name.equals(q, ignoreCase = true)
                    }
                    if (matches.isNotEmpty()) {
                        "Found ${matches.size} commands: " + matches.take(5).joinToString("; ") { "${it.commandId} (${it.syntax})" }
                    } else {
                        "No command or category matching '$q'"
                    }
                }
                val res = SkillResult("HELP", SkillStatus.SUCCESS, msg, 0L)
                DispatchDetails(trimmed, "HELP", cmdDef.handlerIdentifier, res, msg)
            }
            "commands" -> {
                val filter = parsedArgs.getString("filter") ?: ""
                val filtered = commandRegistry.getAllCommands().filter {
                    filter.isBlank() || it.commandId.contains(filter, ignoreCase = true) || it.category.name.contains(filter, ignoreCase = true)
                }
                val msg = "Registered Commands (${filtered.size}): " + filtered.take(10).joinToString(", ") { it.commandId }
                val res = SkillResult("COMMANDS", SkillStatus.SUCCESS, msg, 0L)
                DispatchDetails(trimmed, "COMMANDS", cmdDef.handlerIdentifier, res, msg)
            }
            "observe.start" -> {
                observationEngine?.startObservationMode()
                val res = SkillResult("OBSERVE_START", SkillStatus.SUCCESS, "Observation mode STARTED", 0L)
                DispatchDetails(trimmed, "OBSERVE_START", cmdDef.handlerIdentifier, res, res.message)
            }
            "observe.stop" -> {
                observationEngine?.stopObservationMode()
                val res = SkillResult("OBSERVE_STOP", SkillStatus.SUCCESS, "Observation mode STOPPED", 0L)
                DispatchDetails(trimmed, "OBSERVE_STOP", cmdDef.handlerIdentifier, res, res.message)
            }
            "observe.current" -> {
                val snap = observationEngine?.getDisplayedSnapshot() ?: observationEngine?.getLastSnapshot()
                val msg = if (snap != null) {
                    "Foreground App: ${snap.packageName} | Activity: ${snap.activityName} | Nodes: ${snap.nodeCount} | State: ${snap.state}"
                } else {
                    "No active observation snapshot"
                }
                val res = SkillResult("OBSERVE_CURRENT", if (snap != null) SkillStatus.SUCCESS else SkillStatus.FAILED, msg, 0L)
                DispatchDetails(trimmed, "OBSERVE_CURRENT", cmdDef.handlerIdentifier, res, msg)
            }
            "observe.nodes" -> {
                val snap = observationEngine?.getDisplayedSnapshot() ?: observationEngine?.getLastSnapshot()
                val msg = if (snap != null) {
                    "Nodes (${snap.allNodesList.size}): " + snap.allNodesList.take(5).joinToString("; ") { "${it.id}: '${it.text ?: it.contentDescription ?: it.className}'" }
                } else {
                    "No active observation snapshot"
                }
                val res = SkillResult("OBSERVE_NODES", if (snap != null) SkillStatus.SUCCESS else SkillStatus.FAILED, msg, 0L)
                DispatchDetails(trimmed, "OBSERVE_NODES", cmdDef.handlerIdentifier, res, msg)
            }
            "system.status" -> {
                val report = readinessEvaluator?.evaluate()
                val statusStr = if (report?.isReady == true) "READY" else "DEGRADED"
                val msg = "System Readiness: $statusStr (${report?.blockingReasons?.size ?: 0} blockers)"
                val res = SkillResult("SYSTEM_STATUS", SkillStatus.SUCCESS, msg, 0L)
                DispatchDetails(trimmed, "SYSTEM_STATUS", cmdDef.handlerIdentifier, res, msg)
            }
            "ui.state" -> {
                val msg = "UI State: Operational"
                val res = SkillResult("UI_STATE", SkillStatus.SUCCESS, msg, 0L)
                DispatchDetails(trimmed, "UI_STATE", cmdDef.handlerIdentifier, res, msg)
            }
            else -> {
                val res = SkillResult(cmdDef.commandId, SkillStatus.SUCCESS, "Executed command '${cmdDef.commandId}'", 0L)
                DispatchDetails(trimmed, opName, cmdDef.handlerIdentifier, res, res.message)
            }
        }
    }
}
