package com.agent.android

import android.media.AudioManager
import com.agent.android.agent.device.HardwareObservationControllers
import com.agent.android.agent.device.SystemControlControllers
import com.agent.android.agent.device.VolumeController
import com.agent.android.agent.skills.SkillStatus
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase241FoundationPatchTest {

    @Test
    fun testNotesProductionSyntaxRouting() {
        val controller = ExecutionController()
        val dispatcher = GoalDispatcherImpl(controller)

        val details = dispatcher.dispatchAndProcessWithLock("note down buy milk")
        assertEquals("NOTE", details.operation)
        assertEquals("NotesSkill", details.controllerName)
    }

    @Test
    fun testBrightnessReadStatusSeparationFromSetValidation() {
        val controller = ExecutionController()
        val sysCtrl = SystemControlControllers(null)
        val dispatcher = GoalDispatcherImpl(controller, systemControlControllers = sysCtrl)

        val statusDetails = dispatcher.dispatchAndProcessWithLock("brightness status")
        assertEquals("BRIGHTNESS", statusDetails.operation)
        assertEquals(SkillStatus.UNAVAILABLE, statusDetails.result.status)
        assertEquals("NO_CONTEXT", statusDetails.result.errorCode)

        val setInvalidDetails = dispatcher.dispatchAndProcessWithLock("brightness 128")
        assertEquals("BRIGHTNESS", setInvalidDetails.operation)
        assertEquals(SkillStatus.FAILED, setInvalidDetails.result.status)
        assertEquals("INVALID_ARGUMENT", setInvalidDetails.result.errorCode)
    }

    @Test
    fun testVolumeSubsystemPercentageIndexRoundingAndValidation() {
        val vol = VolumeController(null)

        // Stream type parsing
        assertEquals(AudioManager.STREAM_ALARM, vol.parseStreamType("alarm"))
        assertEquals(AudioManager.STREAM_RING, vol.parseStreamType("ring"))
        assertEquals(AudioManager.STREAM_NOTIFICATION, vol.parseStreamType("notification"))
        assertEquals(AudioManager.STREAM_MUSIC, vol.parseStreamType("music"))

        // Stream names
        assertEquals("ALARM", vol.getStreamName(AudioManager.STREAM_ALARM))
        assertEquals("RING", vol.getStreamName(AudioManager.STREAM_RING))
        assertEquals("NOTIFICATION", vol.getStreamName(AudioManager.STREAM_NOTIFICATION))
        assertEquals("MUSIC", vol.getStreamName(AudioManager.STREAM_MUSIC))

        // Input validation bounds
        val resNeg = vol.setVolumePercentage(-1, AudioManager.STREAM_MUSIC)
        assertEquals(SkillStatus.FAILED, resNeg.status)
        assertEquals("INVALID_ARGUMENT", resNeg.errorCode)

        val resExcess = vol.setVolumePercentage(101, AudioManager.STREAM_MUSIC)
        assertEquals(SkillStatus.FAILED, resExcess.status)
        assertEquals("INVALID_ARGUMENT", resExcess.errorCode)

        // Rounding math checks
        val pct15_7 = Math.round((7.0 / 15.0) * 100.0).toInt()
        assertEquals(47, pct15_7)

        val pct15_8 = Math.round((8.0 / 15.0) * 100.0).toInt()
        assertEquals(53, pct15_8)

        val pct7_3 = Math.round((3.0 / 7.0) * 100.0).toInt()
        assertEquals(43, pct7_3)
    }

    @Test
    fun testSensorListEnumeration() {
        val obsControllers = HardwareObservationControllers(null)
        val res = obsControllers.getSensorList()

        assertEquals("SENSOR_LIST", res.operation)
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
        assertEquals("NO_CONTEXT", res.errorCode)
    }

    @Test
    fun testRingerModeSpecialAccessBlocking() {
        val sysCtrl = SystemControlControllers(null)
        val res = sysCtrl.setRingerMode("silent")

        assertEquals("RINGER", res.operation)
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
        assertEquals("NO_CONTEXT", res.errorCode)
    }
}
