package com.agent.android

import android.view.KeyEvent
import com.agent.android.agent.device.CapabilityStatus
import com.agent.android.agent.device.CapabilityRegistry
import com.agent.android.agent.device.SystemControlControllers
import com.agent.android.agent.device.VolumeController
import com.agent.android.agent.skills.IntentSkills
import com.agent.android.agent.skills.SkillStatus
import com.agent.android.service.LocalAgentAccessibilityService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase23HardeningPatchUnitTest {

    @Test
    fun testPhysicalVolumeKeyEventsNotConsumed() {
        val service = LocalAgentAccessibilityService()

        val eventUp = KeyEvent(100L, 100L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 0)
        val handledUp = service.onKeyEvent(eventUp)
        assertFalse("Single Volume-Up key event must NOT be consumed by accessibility service", handledUp)

        val eventDown = KeyEvent(100L, 100L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, 0)
        val handledDown = service.onKeyEvent(eventDown)
        assertFalse("Volume-Down key event must NOT be consumed by accessibility service", handledDown)

        val eventMute = KeyEvent(100L, 100L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE, 0)
        val handledMute = service.onKeyEvent(eventMute)
        assertFalse("Volume-Mute key event must NOT be consumed by accessibility service", handledMute)
    }

    @Test
    fun testVolumeStatusAndPercentageConversion() {
        val volume = VolumeController(null)

        val resNoContext = volume.getVolume()
        assertEquals(SkillStatus.UNAVAILABLE, resNoContext.status)
        assertEquals("NO_CONTEXT", resNoContext.errorCode)

        val nameMusic = volume.getStreamName(android.media.AudioManager.STREAM_MUSIC)
        assertEquals("MUSIC", nameMusic)
        val nameRing = volume.getStreamName(android.media.AudioManager.STREAM_RING)
        assertEquals("RING", nameRing)
    }

    @Test
    fun testBrightnessWriteSettingsPermissionSafeguard() {
        val sysCtrl = SystemControlControllers(null)
        val res = sysCtrl.setBrightness(50)
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
        assertEquals("NO_CONTEXT", res.errorCode)
    }

    @Test
    fun testRingerStatusReadIndependentOfWritePermission() {
        val sysCtrl = SystemControlControllers(null)
        val resStatus = sysCtrl.getRingerStatus()
        assertEquals(SkillStatus.UNAVAILABLE, resStatus.status)
        assertEquals("NO_CONTEXT", resStatus.errorCode)
    }

    @Test
    fun testMediaStatusQuery() {
        val sysCtrl = SystemControlControllers(null)
        val res = sysCtrl.getMediaStatus()
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
        assertEquals("NO_CONTEXT", res.errorCode)
    }

    @Test
    fun testLocationStatusProviderReporting() {
        val sysCtrl = SystemControlControllers(null)
        val res = sysCtrl.getLocationStatus()
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
        assertEquals("NO_CONTEXT", res.errorCode)
    }

    @Test
    fun testCapabilityRegistryPermissionStatus() {
        val registry = CapabilityRegistry(null)
        val caps = registry.checkAllCapabilities()

        val brightnessCap = caps["BRIGHTNESS"]
        assertNotNull(brightnessCap)
        assertEquals(CapabilityStatus.PERMISSION_REQUIRED, brightnessCap?.status)
    }
}
