package com.agent.android.agent.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import androidx.core.content.ContextCompat
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class CameraController(private val context: Context?) {

    fun getCameraStatus(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("CAMERA", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return SkillResult("CAMERA", SkillStatus.UNAVAILABLE, "CameraManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            val count = cm.cameraIdList.size
            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            val msg = "Cameras Detected: $count | Permission: ${if (hasPerm) "GRANTED" else "DENIED"}"
            SkillResult("CAMERA", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("CAMERA", SkillStatus.FAILED, "Error querying camera: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun getCameraPermissionStatus(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("CAMERA", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val status = if (hasPerm) SkillStatus.SUCCESS else SkillStatus.PERMISSION_REQUIRED
        val msg = "CAMERA_PERMISSION: ${if (hasPerm) "GRANTED" else "DENIED"}"
        return SkillResult("CAMERA", status, msg, System.currentTimeMillis() - start)
    }

    fun getCameraList(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("CAMERA", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return SkillResult("CAMERA", SkillStatus.UNAVAILABLE, "CameraManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            val ids = cm.cameraIdList
            val sb = StringBuilder("Camera IDs (${ids.size}): ")
            sb.append(ids.joinToString(", "))
            SkillResult("CAMERA", SkillStatus.SUCCESS, sb.toString(), System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("CAMERA", SkillStatus.FAILED, "Error listing cameras: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }
}
