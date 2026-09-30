package com.agent.android.agent.device

import android.content.Context
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class DeviceStateController(private val context: Context?) {

    private val batteryController = HardwareObservationControllers(context)
    private val networkController = NetworkController(context)
    private val displayController = DisplayController(context)
    private val powerController = PowerStateController(context)

    fun getDeviceSnapshot(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("DEVICE_SNAPSHOT", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val bat = batteryController.getBatteryStatus().message
        val net = networkController.getNetworkStatus().message
        val disp = displayController.getDisplayStatus().message
        val pow = powerController.getPowerStatus().message

        val snapshotMsg = "UNIFIED DEVICE SNAPSHOT:\n- $bat\n- $net\n- $disp\n- $pow"
        return SkillResult("DEVICE_SNAPSHOT", SkillStatus.SUCCESS, snapshotMsg, System.currentTimeMillis() - start)
    }
}
