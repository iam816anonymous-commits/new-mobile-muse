package com.agent.android.agent.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.core.content.ContextCompat
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

data class TorchMappingDiagnostic(
    val backCameraIds: List<String>,
    val frontCameraIds: List<String>,
    val otherCameraIds: List<String>,
    val capabilityExists: Boolean,
    val capabilityPermitted: Boolean,
    val capabilityUsable: Boolean
) {
    val summaryText: String
        get() = "Back Torch: ${if (backCameraIds.isEmpty()) "NONE" else backCameraIds.joinToString()} | " +
                "Front Torch: ${if (frontCameraIds.isEmpty()) "NONE" else frontCameraIds.joinToString()} | " +
                "EXISTS=$capabilityExists, PERMITTED=$capabilityPermitted, USABLE=$capabilityUsable"
}

class FlashlightController(private val context: Context?) {

    fun getTorchDiagnostic(): TorchMappingDiagnostic {
        val backIds = mutableListOf<String>()
        val frontIds = mutableListOf<String>()
        val otherIds = mutableListOf<String>()

        val cameraManager = context?.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager != null) {
            try {
                for (id in cameraManager.cameraIdList) {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    if (hasFlash) {
                        val facing = chars.get(CameraCharacteristics.LENS_FACING)
                        when (facing) {
                            CameraCharacteristics.LENS_FACING_BACK -> backIds.add(id)
                            CameraCharacteristics.LENS_FACING_FRONT -> frontIds.add(id)
                            else -> otherIds.add(id)
                        }
                    }
                }
            } catch (ignored: Exception) {}
        }

        val capabilityExists = backIds.isNotEmpty() || frontIds.isNotEmpty() || otherIds.isNotEmpty()
        val capabilityPermitted = context != null && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val capabilityUsable = capabilityExists && capabilityPermitted

        return TorchMappingDiagnostic(
            backCameraIds = backIds,
            frontCameraIds = frontIds,
            otherCameraIds = otherIds,
            capabilityExists = capabilityExists,
            capabilityPermitted = capabilityPermitted,
            capabilityUsable = capabilityUsable
        )
    }

    fun setFlashlight(enable: Boolean): SkillResult {
        return setFlashlightTarget(if (enable) "back" else "off", enable)
    }

    fun setFlashlightTarget(targetInput: String, enable: Boolean = true): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) {
            return SkillResult("FLASHLIGHT", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")
        }

        val target = targetInput.trim().lowercase()
        val validTargets = setOf("back", "front", "both", "off", "on", "status")
        if (!validTargets.contains(target)) {
            return SkillResult(
                "FLASHLIGHT",
                SkillStatus.FAILED,
                "Invalid flashlight target: '$targetInput'. Supported targets: back, front, both, off, status",
                System.currentTimeMillis() - start,
                "INVALID_TARGET"
            )
        }

        val diag = getTorchDiagnostic()

        if (target == "status") {
            val statusMsg = "Flashlight Status: ${diag.summaryText}"
            return SkillResult("FLASHLIGHT", SkillStatus.SUCCESS, statusMsg, System.currentTimeMillis() - start)
        }

        if (!diag.capabilityExists) {
            return SkillResult(
                "FLASHLIGHT",
                SkillStatus.UNSUPPORTED,
                "Camera torch hardware unavailable on device",
                System.currentTimeMillis() - start,
                "NO_TORCH"
            )
        }

        if (!diag.capabilityPermitted) {
            return SkillResult(
                "FLASHLIGHT",
                SkillStatus.PERMISSION_REQUIRED,
                "CAMERA permission required for torch",
                System.currentTimeMillis() - start,
                "CAMERA_PERMISSION_REQUIRED"
            )
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return SkillResult("FLASHLIGHT", SkillStatus.UNAVAILABLE, "CameraManager unavailable", System.currentTimeMillis() - start, "NO_CAMERA_MANAGER")

        return try {
            val allTorchIds = (diag.backCameraIds + diag.frontCameraIds + diag.otherCameraIds).distinct()

            when (target) {
                "off" -> {
                    for (id in allTorchIds) {
                        try { cameraManager.setTorchMode(id, false) } catch (ignored: Exception) {}
                    }
                    SkillResult("FLASHLIGHT", SkillStatus.SUCCESS, "Flashlight set to OFF on all camera torches", System.currentTimeMillis() - start)
                }
                "front" -> {
                    if (diag.frontCameraIds.isEmpty()) {
                        return SkillResult(
                            "FLASHLIGHT",
                            SkillStatus.UNSUPPORTED,
                            "Torch target 'front' not available on this device",
                            System.currentTimeMillis() - start,
                            "TORCH_NOT_FOUND"
                        )
                    }
                    for (id in diag.frontCameraIds) {
                        cameraManager.setTorchMode(id, enable)
                    }
                    val actionStr = if (enable) "ON" else "OFF"
                    SkillResult("FLASHLIGHT", SkillStatus.SUCCESS, "Front flashlight set to $actionStr (Cameras: ${diag.frontCameraIds.joinToString()})", System.currentTimeMillis() - start)
                }
                "back", "on" -> {
                    val targetBackIds = if (diag.backCameraIds.isNotEmpty()) diag.backCameraIds else allTorchIds
                    if (targetBackIds.isEmpty()) {
                        return SkillResult(
                            "FLASHLIGHT",
                            SkillStatus.UNSUPPORTED,
                            "Torch target 'back' not available on this device",
                            System.currentTimeMillis() - start,
                            "TORCH_NOT_FOUND"
                        )
                    }
                    for (id in targetBackIds) {
                        cameraManager.setTorchMode(id, enable)
                    }
                    val actionStr = if (enable) "ON" else "OFF"
                    SkillResult("FLASHLIGHT", SkillStatus.SUCCESS, "Back flashlight set to $actionStr (Cameras: ${targetBackIds.joinToString()})", System.currentTimeMillis() - start)
                }
                "both" -> {
                    if (allTorchIds.isEmpty()) {
                        return SkillResult(
                            "FLASHLIGHT",
                            SkillStatus.UNSUPPORTED,
                            "No camera torches available on this device",
                            System.currentTimeMillis() - start,
                            "NO_TORCH"
                        )
                    }
                    for (id in allTorchIds) {
                        cameraManager.setTorchMode(id, enable)
                    }
                    val actionStr = if (enable) "ON" else "OFF"
                    SkillResult("FLASHLIGHT", SkillStatus.SUCCESS, "Both front and back flashlights set to $actionStr (Cameras: ${allTorchIds.joinToString()})", System.currentTimeMillis() - start)
                }
                else -> {
                    SkillResult("FLASHLIGHT", SkillStatus.FAILED, "Unsupported flashlight target: '$target'", System.currentTimeMillis() - start, "INVALID_TARGET")
                }
            }
        } catch (e: SecurityException) {
            SkillResult("FLASHLIGHT", SkillStatus.PERMISSION_REQUIRED, "CAMERA permission required for torch: ${e.message}", System.currentTimeMillis() - start, "CAMERA_PERMISSION_REQUIRED")
        } catch (e: Exception) {
            SkillResult("FLASHLIGHT", SkillStatus.FAILED, "Flashlight control error: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_ERROR")
        }
    }
}
