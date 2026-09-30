package com.agent.android

import com.agent.android.agent.device.HardwareObservationControllers
import com.agent.android.agent.device.SystemControlControllers
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
