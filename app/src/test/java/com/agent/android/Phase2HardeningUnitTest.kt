package com.agent.android

import com.agent.android.agent.device.HapticController
import com.agent.android.agent.device.HardwareObservationControllers
import com.agent.android.agent.device.VolumeController
import com.agent.android.agent.skills.IntentSkills
import com.agent.android.agent.skills.NotesSkill
import com.agent.android.agent.skills.SkillStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2HardeningUnitTest {

    @Test
    fun testVibrationValidation() {
        val controller = HapticController(null)

        val resNegative = controller.vibrate(-500)
        assertEquals(SkillStatus.FAILED, resNegative.status)
        assertEquals("INVALID_ARGUMENT", resNegative.errorCode)

        val resZero = controller.vibrate(0)
        assertEquals(SkillStatus.FAILED, resZero.status)
        assertEquals("INVALID_ARGUMENT", resZero.errorCode)

        val resExcessive = controller.vibrate(2001)
        assertEquals(SkillStatus.FAILED, resExcessive.status)
        assertEquals("INVALID_ARGUMENT", resExcessive.errorCode)

        val resValid = controller.vibrate(500)
        assertEquals(SkillStatus.UNAVAILABLE, resValid.status) // Fails safely at context check
        assertEquals("NO_CONTEXT", resValid.errorCode)
    }

    @Test
    fun testVolumeValidation() {
        val controller = VolumeController(null)

        val resInvalidPercent = controller.setVolumePercentage(-10)
        assertEquals(SkillStatus.FAILED, resInvalidPercent.status)
        assertEquals("INVALID_ARGUMENT", resInvalidPercent.errorCode)

        val resExcessPercent = controller.setVolumePercentage(150)
        assertEquals(SkillStatus.FAILED, resExcessPercent.status)
        assertEquals("INVALID_ARGUMENT", resExcessPercent.errorCode)

        val resValidPercent = controller.setVolumePercentage(50)
        assertEquals(SkillStatus.UNAVAILABLE, resValidPercent.status)
        assertEquals("NO_CONTEXT", resValidPercent.errorCode)
    }

    @Test
    fun testIntentValidationAndResolution() {
        val intents = IntentSkills(android.content.ContextWrapper(null))

        val resTimerInvalid = intents.setTimer(-5)
        assertEquals(SkillStatus.FAILED, resTimerInvalid.status)
        assertEquals("INVALID_ARGUMENT", resTimerInvalid.errorCode)

        val resAlarmInvalid = intents.setAlarm(25, 60)
        assertEquals(SkillStatus.FAILED, resAlarmInvalid.status)
        assertEquals("INVALID_ARGUMENT", resAlarmInvalid.errorCode)

        val resSearchInvalid = intents.webSearch("   ")
        assertEquals(SkillStatus.FAILED, resSearchInvalid.status)
        assertEquals("INVALID_ARGUMENT", resSearchInvalid.errorCode)
    }

    @Test
    fun testNotesSkillPermissionAndValidation() {
        val notes = NotesSkill(null)
        val resBlank = notes.addNote("")
        assertEquals(SkillStatus.FAILED, resBlank.status)
        assertEquals("EMPTY_NOTE", resBlank.errorCode)
    }

    @Test
    fun testSensorSamplingNoContextSafeguard() {
        val obs = HardwareObservationControllers(null)
        val res = obs.sampleSensor(android.hardware.Sensor.TYPE_ACCELEROMETER, "Accelerometer")
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
        assertEquals("NO_CONTEXT", res.errorCode)

        // Controller remains usable for subsequent calls
        val resGyro = obs.sampleSensor(android.hardware.Sensor.TYPE_GYROSCOPE, "Gyroscope")
        assertEquals(SkillStatus.UNAVAILABLE, resGyro.status)
        assertEquals("NO_CONTEXT", resGyro.errorCode)
    }

    @Test
    fun testDeviceDiagnosticsGeneration() {
        val obs = HardwareObservationControllers(null)
        val report = obs.runDeviceDiagnostics()
        assertNotNull(report)
        assertNotNull(report.androidVersion)
        assertNotNull(report.deviceModel)
        assertEquals(5, report.sensors.size)

        // Verify sensor diagnostic entries handle null context safely
        val sensorNames = report.sensors.map { it.name }
        assertTrue(sensorNames.contains("Accelerometer"))
        assertTrue(sensorNames.contains("Gyroscope"))
        assertTrue(sensorNames.contains("Proximity"))
        assertTrue(sensorNames.contains("Ambient Light"))
        assertTrue(sensorNames.contains("Magnetometer"))

        for (s in report.sensors) {
            assertFalse(s.isAvailable)
            assertEquals("N/A", s.vendor)
        }
    }
}
