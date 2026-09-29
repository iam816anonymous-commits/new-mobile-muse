package com.agent.android.agent.device

import android.content.Context
import android.hardware.camera2.CameraManager
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class FlashlightController(private val context: Context?) {

    fun setFlashlight(enable: Boolean): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) {
            return SkillResult("FLASHLIGHT", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")
        }
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraManager == null || cameraId == null) {
                return SkillResult("FLASHLIGHT", SkillStatus.UNSUPPORTED, "Camera torch unavailable on device", System.currentTimeMillis() - start, "NO_TORCH")
            }
            cameraManager.setTorchMode(cameraId, enable)
            SkillResult("FLASHLIGHT", SkillStatus.SUCCESS, "Flashlight set to ${if (enable) "ON" else "OFF"}", System.currentTimeMillis() - start)
        } catch (e: SecurityException) {
            SkillResult("FLASHLIGHT", SkillStatus.PERMISSION_REQUIRED, "Camera permission required for torch", System.currentTimeMillis() - start, "PERMISSION_DENIED")
        } catch (e: Exception) {
            SkillResult("FLASHLIGHT", SkillStatus.FAILED, "Flashlight control error: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_ERROR")
        }
    }
}
