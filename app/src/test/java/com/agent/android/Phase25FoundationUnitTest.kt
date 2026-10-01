package com.agent.android

import com.agent.android.agent.device.AccessibilityActionController
import com.agent.android.agent.device.ActionRequest
import com.agent.android.agent.device.AppDiscoveryController
import com.agent.android.agent.device.BackgroundExecutionPolicy
import com.agent.android.agent.device.CameraController
import com.agent.android.agent.device.ClipboardController
import com.agent.android.agent.device.DeviceStateController
import com.agent.android.agent.device.DisplayController
import com.agent.android.agent.device.FileAccessController
import com.agent.android.agent.device.InputStateController
import com.agent.android.agent.device.InteractionVisualizer
import com.agent.android.agent.device.LocationController
import com.agent.android.agent.device.LocationReadinessStatus
import com.agent.android.agent.device.NetworkController
import com.agent.android.agent.device.NotificationController
import com.agent.android.agent.device.PowerStateController
import com.agent.android.agent.device.ScreenCaptureController
import com.agent.android.agent.device.TextInputController
import com.agent.android.agent.device.UsageStatsController
import com.agent.android.agent.skills.SkillStatus
import com.agent.android.commands.CommandRegistry
import com.agent.android.commands.CommandStatus
import com.agent.android.permissions.PermissionRegistry
import com.agent.android.test.FoundationTestRegistry
import com.agent.android.test.model.TestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase25FoundationUnitTest {

    @Test
    fun testPermissionRegistryIntegrity() {
        val permRegistry = PermissionRegistry()
        val allPerms = permRegistry.getAllPermissions()

        val permIds = allPerms.map { it.id }
        assertEquals("No duplicate permission IDs permitted", permIds.size, permIds.toSet().size)

        for (p in allPerms) {
            assertNotNull("Permission ID must not be null", p.id)
            assertTrue("Permission ID must not be blank", p.id.isNotBlank())
            assertNotNull("Permission Category must not be null", p.category)
        }
    }

    @Test
    fun testCommandRegistryTestCoverageIntegrity() {
        val commandRegistry = CommandRegistry()
        val testRegistry = FoundationTestRegistry()

        val allCommands = commandRegistry.getAllCommands()
        val implementedCommands = allCommands.filter { it.status == CommandStatus.IMPLEMENTED }
        val testCases = testRegistry.getAllTestCases()
        val testedCommandIds = testCases.map { it.commandId }.toSet()

        val uncovered = implementedCommands.filter { !testedCommandIds.contains(it.commandId) }

        assertTrue(
            "Every implemented command in CommandRegistry must have corresponding test coverage in FoundationTestRegistry. Uncovered commands: ${uncovered.map { it.commandId }}",
            uncovered.isEmpty()
        )

        val testIds = testCases.map { it.id }
        assertEquals("No duplicate test IDs allowed in FoundationTestRegistry", testIds.size, testIds.toSet().size)
    }

    @Test
    fun testCommandPermissionReferenceIntegrity() {
        val commandRegistry = CommandRegistry()
        val permissionRegistry = PermissionRegistry()

        val validPermIds = permissionRegistry.getAllPermissions().map { it.id }.toSet()
        val validPermAndroidIds = permissionRegistry.getAllPermissions().map { it.androidIdentifier }.toSet()

        for (cmd in commandRegistry.getAllCommands()) {
            val req = cmd.requirement
            for (p in req.requiredPermissions) {
                assertTrue(
                    "Command '${cmd.commandId}' references unregistered permission '$p'",
                    validPermIds.contains(p) || validPermAndroidIds.contains(p)
                )
            }
            for (sa in req.requiredSpecialAccess) {
                assertTrue(
                    "Command '${cmd.commandId}' references unregistered special access '$sa'",
                    validPermIds.contains(sa) || validPermAndroidIds.contains(sa) || sa.isNotBlank()
                )
            }
        }
    }

    @Test
    fun testClipboardControllerNullContextSafeguard() {
        val ctrl = ClipboardController(null)
        val resStatus = ctrl.getClipboardStatus()
        assertEquals(SkillStatus.UNAVAILABLE, resStatus.status)
        assertEquals("NO_CONTEXT", resStatus.errorCode)

        val resRead = ctrl.readClipboard()
        assertEquals(SkillStatus.UNAVAILABLE, resRead.status)

        val resWrite = ctrl.writeClipboard("test")
        assertEquals(SkillStatus.UNAVAILABLE, resWrite.status)

        val resClear = ctrl.clearClipboard()
        assertEquals(SkillStatus.UNAVAILABLE, resClear.status)
    }

    @Test
    fun testNotificationControllerNullContextSafeguard() {
        val ctrl = NotificationController(null)
        val res = ctrl.getNotificationStatus()
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
    }

    @Test
    fun testUsageStatsControllerNullContextSafeguard() {
        val ctrl = UsageStatsController(null)
        val res = ctrl.getCurrentForegroundApp()
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
    }

    @Test
    fun testAccessibilityActionControllerContracts() {
        val ctrl = AccessibilityActionController()
        val tapRes = ctrl.executeContractAction(ActionRequest("tap", 100f, 200f))
        assertEquals(SkillStatus.SUCCESS, tapRes.status)

        val backRes = ctrl.executeContractAction(ActionRequest("back"))
        assertEquals(SkillStatus.SUCCESS, backRes.status)

        val invalidRes = ctrl.executeContractAction(ActionRequest("invalid_gesture"))
        assertEquals(SkillStatus.FAILED, invalidRes.status)
        assertEquals("INVALID_ACTION", invalidRes.errorCode)
    }

    @Test
    fun testTextInputControllerContracts() {
        val ctrl = TextInputController()
        val setRes = ctrl.executeTextContract("setText", "hello")
        assertEquals(SkillStatus.SUCCESS, setRes.status)

        val invalidRes = ctrl.executeTextContract("invalid_action")
        assertEquals(SkillStatus.FAILED, invalidRes.status)
        assertEquals("INVALID_ACTION", invalidRes.errorCode)
    }

    @Test
    fun testDisplayControllerNullContextSafeguard() {
        val ctrl = DisplayController(null)
        val res = ctrl.getDisplayStatus()
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
    }

    @Test
    fun testScreenCaptureControllerNullContextSafeguard() {
        val ctrl = ScreenCaptureController(null)
        val res = ctrl.getScreenCaptureStatus()
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
    }

    @Test
    fun testInteractionVisualizer() {
        val vis = InteractionVisualizer()
        val showRes = vis.showIndicator(100f, 200f, "tap")
        assertEquals(SkillStatus.SUCCESS, showRes.status)

        val dismissRes = vis.dismissIndicator()
        assertEquals(SkillStatus.SUCCESS, dismissRes.status)
    }

    @Test
    fun testInputStateControllerNullContextSafeguard() {
        val ctrl = InputStateController(null)
        val kbdRes = ctrl.getKeyboardStatus()
        assertEquals(SkillStatus.UNAVAILABLE, kbdRes.status)
    }

    @Test
    fun testCameraControllerNullContextSafeguard() {
        val ctrl = CameraController(null)
        val res = ctrl.getCameraStatus()
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
    }

    @Test
    fun testFileAccessControllerNullContextSafeguard() {
        val ctrl = FileAccessController(null)
        val res = ctrl.getFileAccessStatus()
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
    }

    @Test
    fun testLocationControllerNullContextSafeguard() {
        val ctrl = LocationController(null)
        val res = ctrl.getLocationProviders()
        assertEquals(SkillStatus.UNAVAILABLE, res.status)

        val fixRes = ctrl.testLocationFix()
        assertEquals(SkillStatus.UNAVAILABLE, fixRes.status)
    }

    @Test
    fun testLocationControllerDiagnosticsReport() {
        val ctrl = LocationController(null)
        val diag = ctrl.diagnoseLocation()
        assertEquals(LocationReadinessStatus.UNAVAILABLE, diag.status)
        assertEquals("Context unavailable", diag.rootCauseExplanation)
        assertEquals(false, diag.finePermissionGranted)
        assertEquals(false, diag.coarsePermissionGranted)
    }

    @Test
    fun testNetworkControllerNullContextSafeguard() {
        val ctrl = NetworkController(null)
        val res = ctrl.getNetworkStatus()
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
    }

    @Test
    fun testPowerStateControllerNullContextSafeguard() {
        val ctrl = PowerStateController(null)
        val res = ctrl.getPowerStatus()
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
    }

    @Test
    fun testBackgroundExecutionPolicy() {
        val policy = BackgroundExecutionPolicy()
        val res = policy.getPolicyStatus()
        assertEquals(SkillStatus.SUCCESS, res.status)
    }

    @Test
    fun testAppDiscoveryControllerNullContextSafeguard() {
        val ctrl = AppDiscoveryController(null)
        val listRes = ctrl.listApps()
        assertEquals(SkillStatus.UNAVAILABLE, listRes.status)
    }

    @Test
    fun testDeviceStateControllerNullContextSafeguard() {
        val ctrl = DeviceStateController(null)
        val res = ctrl.getDeviceSnapshot()
        assertEquals(SkillStatus.UNAVAILABLE, res.status)
    }
}
