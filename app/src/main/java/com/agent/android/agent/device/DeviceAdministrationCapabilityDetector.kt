package com.agent.android.agent.device

import android.app.admin.DevicePolicyManager
import android.content.Context
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class DeviceAdministrationCapabilityDetector(private val context: Context?) {

    fun getDeviceAdminState(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("DEVICE_ADMIN", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return SkillResult("DEVICE_ADMIN", SkillStatus.UNAVAILABLE, "DevicePolicyManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            val isDeviceOwner = dpm.isDeviceOwnerApp(context.packageName)
            val isProfileOwner = dpm.isProfileOwnerApp(context.packageName)
            val isAdminActive = dpm.activeAdmins?.any { it.packageName == context.packageName } == true

            val classification = when {
                isDeviceOwner -> "DEVICE_OWNER"
                isProfileOwner -> "PROFILE_OWNER"
                isAdminActive -> "DEVICE_ADMIN"
                else -> "NORMAL_APP"
            }

            val msg = "APP_OWNERSHIP_CLASSIFICATION: $classification (IsAdmin: $isAdminActive, IsDeviceOwner: $isDeviceOwner, IsProfileOwner: $isProfileOwner)"
            SkillResult("DEVICE_ADMIN", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("DEVICE_ADMIN", SkillStatus.FAILED, "Error querying device admin state: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }
}
