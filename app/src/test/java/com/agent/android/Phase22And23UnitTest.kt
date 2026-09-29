package com.agent.android

import com.agent.android.agent.device.CapabilityRegistry
import com.agent.android.agent.device.SystemControlControllers
import com.agent.android.agent.device.VolumeController
import com.agent.android.agent.skills.IntentSkills
import com.agent.android.agent.skills.SkillStatus
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase22And23UnitTest {

    @Test
    fun testTimerAndAlarmValidation() {
        val intents = IntentSkills(android.content.ContextWrapper(null))

        val timerZero = intents.setTimer(0)
        assertEquals(SkillStatus.FAILED, timerZero.status)
        assertEquals("INVALID_ARGUMENT", timerZero.errorCode)

        val timerNeg = intents.setTimer(-10)
        assertEquals(SkillStatus.FAILED, timerNeg.status)
        assertEquals("INVALID_ARGUMENT", timerNeg.errorCode)

        val alarmInvalidHour = intents.setAlarm(25, 0)
        assertEquals(SkillStatus.FAILED, alarmInvalidHour.status)
        assertEquals("INVALID_ARGUMENT", alarmInvalidHour.errorCode)

        val alarmInvalidMin = intents.setAlarm(12, 60)
        assertEquals(SkillStatus.FAILED, alarmInvalidMin.status)
        assertEquals("INVALID_ARGUMENT", alarmInvalidMin.errorCode)
    }

    @Test
    fun testVolumeStreamTypeAndPercentageConversion() {
        val volume = VolumeController(null)

        val musicType = volume.parseStreamType("music")
        assertEquals(android.media.AudioManager.STREAM_MUSIC, musicType)

        val ringType = volume.parseStreamType("ring")
        assertEquals(android.media.AudioManager.STREAM_RING, ringType)

        val alarmType = volume.parseStreamType("alarm")
        assertEquals(android.media.AudioManager.STREAM_ALARM, alarmType)

        val resInvalid = volume.setVolumePercentage(-5)
        assertEquals(SkillStatus.FAILED, resInvalid.status)
        assertEquals("INVALID_ARGUMENT", resInvalid.errorCode)

        val resExcess = volume.setVolumePercentage(105)
        assertEquals(SkillStatus.FAILED, resExcess.status)
        assertEquals("INVALID_ARGUMENT", resExcess.errorCode)
    }

    @Test
    fun testBrightnessAndScreenTimeoutValidation() {
        val sysCtrl = SystemControlControllers(null)

        val brightInvalid = sysCtrl.setBrightness(-10)
        assertEquals(SkillStatus.FAILED, brightInvalid.status)
        assertEquals("INVALID_ARGUMENT", brightInvalid.errorCode)

        val timeoutInvalid = sysCtrl.setScreenTimeout(999)
        assertEquals(SkillStatus.FAILED, timeoutInvalid.status)
        assertEquals("INVALID_ARGUMENT", timeoutInvalid.errorCode)

        val timeoutValid = sysCtrl.setScreenTimeout(30)
        assertEquals(SkillStatus.UNAVAILABLE, timeoutValid.status)
        assertEquals("NO_CONTEXT", timeoutValid.errorCode)
    }

    @Test
    fun testRingerAndMediaValidation() {
        val sysCtrl = SystemControlControllers(null)

        val ringerInvalid = sysCtrl.setRingerMode("unknown_mode")
        assertEquals(SkillStatus.FAILED, ringerInvalid.status)
        assertEquals("INVALID_ARGUMENT", ringerInvalid.errorCode)

        val ringerValidNoCtx = sysCtrl.setRingerMode("normal")
        assertEquals(SkillStatus.UNAVAILABLE, ringerValidNoCtx.status)
        assertEquals("NO_CONTEXT", ringerValidNoCtx.errorCode)

        val mediaInvalid = sysCtrl.dispatchMediaKey("invalid_action")
        assertEquals(SkillStatus.FAILED, mediaInvalid.status)
        assertEquals("INVALID_ARGUMENT", mediaInvalid.errorCode)

        val mediaValidNoCtx = sysCtrl.dispatchMediaKey("play")
        assertEquals(SkillStatus.UNAVAILABLE, mediaValidNoCtx.status)
        assertEquals("NO_CONTEXT", mediaValidNoCtx.errorCode)
    }

    @Test
    fun testDeviceInfoAndLocationReadSafeguards() {
        val sysCtrl = SystemControlControllers(null)

        val devInfo = sysCtrl.getDeviceInfo()
        assertEquals(SkillStatus.SUCCESS, devInfo.status)
        assertNotNull(devInfo.message)

        val locStatus = sysCtrl.getLocationStatus()
        assertEquals(SkillStatus.UNAVAILABLE, locStatus.status)
        assertEquals("NO_CONTEXT", locStatus.errorCode)
    }

    @Test
    fun testCapabilityRegistryNullContext() {
        val registry = CapabilityRegistry(null)
        val caps = registry.checkAllCapabilities()

        assertNotNull(caps["FLASHLIGHT"])
        assertNotNull(caps["VIBRATION"])
        assertNotNull(caps["VOLUME"])
        assertTrue(caps.size >= 12)
    }

    @Test
    fun testGoalDispatcherPhase23Commands() {
        val controller = ExecutionController()
        val sysCtrl = SystemControlControllers(null)
        val dispatcher = GoalDispatcherImpl(controller, systemControlControllers = sysCtrl)

        val brightDetails = dispatcher.dispatchAndProcessWithLock("brightness 50")
        assertEquals("BRIGHTNESS", brightDetails.operation)
        assertEquals("SystemControlControllers", brightDetails.controllerName)

        val ringerDetails = dispatcher.dispatchAndProcessWithLock("ringer normal")
        assertEquals("RINGER", ringerDetails.operation)

        val infoDetails = dispatcher.dispatchAndProcessWithLock("device info")
        assertEquals("DEVICE_INFO", infoDetails.operation)
        assertEquals(SkillStatus.SUCCESS, infoDetails.result.status)
    }
}
