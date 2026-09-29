package com.agent.android

import com.agent.android.agent.device.ConnectivityControllers
import com.agent.android.agent.device.FlashlightController
import com.agent.android.agent.device.HapticController
import com.agent.android.agent.device.HardwareObservationControllers
import com.agent.android.agent.device.VolumeController
import com.agent.android.agent.skills.Phase2DeviceTestHarness
import com.agent.android.agent.skills.SkillStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2DeviceUnitTest {

    @Test
    fun testFlashlightNoContextSafeguard() {
        val flashlight = FlashlightController(null)
        val res = flashlight.setFlashlight(true)
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
        assertEquals("NO_CONTEXT", res.errorCode)
    }

    @Test
    fun testHapticNoContextSafeguard() {
        val haptics = HapticController(null)
        val res = haptics.vibrate(300)
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
        assertEquals("NO_CONTEXT", res.errorCode)
    }

    @Test
    fun testVolumeNoContextSafeguard() {
        val volume = VolumeController(null)
        val res = volume.getVolume()
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
    }

    @Test
    fun testConnectivityNoContextSafeguard() {
        val conn = ConnectivityControllers(null)
        val wifiRes = conn.getWifiStatus()
        assertEquals(SkillStatus.UNAVAILABLE, wifiRes.status)

        val btRes = conn.setBluetooth(true)
        assertEquals(SkillStatus.UNSUPPORTED, btRes.status)
    }

    @Test
    fun testHardwareObservationNoContextSafeguard() {
        val obs = HardwareObservationControllers(null)
        val batRes = obs.getBatteryStatus()
        assertEquals(SkillStatus.UNAVAILABLE, batRes.status)
    }

    @Test
    fun testPhase2DeviceTestHarnessSuite() {
        val harness = Phase2DeviceTestHarness()
        val summary = harness.runAllDeviceTests()
        assertEquals(3, summary.totalCount)
        assertEquals(3, summary.passedCount)
        assertTrue(summary.overallPassed)
    }
}
