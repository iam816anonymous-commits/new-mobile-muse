package com.agent.android

import com.agent.android.agent.skills.app.AppDescriptor
import com.agent.android.agent.skills.app.AppLaunchResult
import com.agent.android.agent.skills.app.AppLaunchStatus
import com.agent.android.agent.skills.app.AppLauncher
import com.agent.android.agent.skills.app.AppLauncherImpl
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2AppLauncherUnitTest {

    private class FakeAppLauncher(private val apps: List<AppDescriptor>) : AppLauncher {
        override fun listInstalledLaunchableApps(): List<AppDescriptor> = apps

        override fun findApp(query: String): List<AppDescriptor> {
            val clean = query.trim()
            if (clean.isEmpty()) return emptyList()
            val packageMatch = apps.filter { it.packageName.equals(clean, ignoreCase = true) }
            if (packageMatch.isNotEmpty()) return packageMatch
            val exactLabelMatch = apps.filter { it.applicationLabel.equals(clean, ignoreCase = true) }
            if (exactLabelMatch.isNotEmpty()) return exactLabelMatch
            return apps.filter { it.applicationLabel.contains(clean, ignoreCase = true) }
        }

        override fun validateApp(query: String): AppLaunchResult {
            val clean = query.trim()
            if (clean.isEmpty()) {
                return AppLaunchResult(query, null, null, null, AppLaunchStatus.INVALID_ARGUMENT, "INVALID_ARGUMENT", "Query empty", 0L)
            }
            val matches = findApp(clean)
            if (matches.isEmpty()) {
                return AppLaunchResult(query, null, null, null, AppLaunchStatus.APP_NOT_FOUND, "APP_NOT_FOUND", "App not found", 0L)
            }
            if (matches.size > 1) {
                return AppLaunchResult(query, null, null, null, AppLaunchStatus.AMBIGUOUS_APP, "AMBIGUOUS_APP", "Ambiguous matches", 0L)
            }
            val app = matches.first()
            if (!app.launchable) {
                return AppLaunchResult(query, app.packageName, app.applicationLabel, app.launcherActivity, AppLaunchStatus.APP_NOT_LAUNCHABLE, "APP_NOT_LAUNCHABLE", "Not launchable", 0L)
            }
            return AppLaunchResult(query, app.packageName, app.applicationLabel, app.launcherActivity, AppLaunchStatus.SUCCESS, null, "Validated", 0L)
        }

        override fun launchApp(query: String): AppLaunchResult {
            val valRes = validateApp(query)
            if (valRes.status != AppLaunchStatus.SUCCESS) return valRes
            return valRes.copy(message = "App launch intent dispatched")
        }
    }

    @Test
    fun testExactPackageResolution() {
        val apps = listOf(
            AppDescriptor("com.android.chrome", "Chrome", true, "com.google.android.apps.chrome.Main"),
            AppDescriptor("com.android.settings", "Settings", true, "com.android.settings.Settings")
        )
        val launcher = FakeAppLauncher(apps)
        val res = launcher.validateApp("com.android.chrome")

        assertEquals(AppLaunchStatus.SUCCESS, res.status)
        assertEquals("com.android.chrome", res.resolvedPackage)
        assertEquals("Chrome", res.resolvedLabel)
    }

    @Test
    fun testExactLabelResolution() {
        val apps = listOf(
            AppDescriptor("com.android.settings", "Settings", true, "com.android.settings.Settings")
        )
        val launcher = FakeAppLauncher(apps)
        val res = launcher.validateApp("Settings")

        assertEquals(AppLaunchStatus.SUCCESS, res.status)
        assertEquals("com.android.settings", res.resolvedPackage)
    }

    @Test
    fun testCaseInsensitiveLabelResolution() {
        val apps = listOf(
            AppDescriptor("com.android.chrome", "Chrome", true, "com.google.android.apps.chrome.Main")
        )
        val launcher = FakeAppLauncher(apps)
        val res = launcher.validateApp("chRoMe")

        assertEquals(AppLaunchStatus.SUCCESS, res.status)
        assertEquals("com.android.chrome", res.resolvedPackage)
    }

    @Test
    fun testAppNotFound() {
        val launcher = FakeAppLauncher(emptyList())
        val res = launcher.validateApp("NonExistentApp")

        assertEquals(AppLaunchStatus.APP_NOT_FOUND, res.status)
        assertEquals("APP_NOT_FOUND", res.errorCode)
    }

    @Test
    fun testAmbiguousAppLabelMatching() {
        val apps = listOf(
            AppDescriptor("com.app.camera.one", "Camera", true, "Activity1"),
            AppDescriptor("com.app.camera.two", "Camera", true, "Activity2")
        )
        val launcher = FakeAppLauncher(apps)
        val res = launcher.validateApp("Camera")

        assertEquals(AppLaunchStatus.AMBIGUOUS_APP, res.status)
        assertEquals("AMBIGUOUS_APP", res.errorCode)
    }

    @Test
    fun testNonLaunchableApp() {
        val apps = listOf(
            AppDescriptor("com.app.serviceonly", "Service App", false, null)
        )
        val launcher = FakeAppLauncher(apps)
        val res = launcher.validateApp("Service App")

        assertEquals(AppLaunchStatus.APP_NOT_LAUNCHABLE, res.status)
        assertEquals("APP_NOT_LAUNCHABLE", res.errorCode)
    }

    @Test
    fun testInvalidEmptyQuery() {
        val launcher = FakeAppLauncher(emptyList())
        val res = launcher.validateApp("   ")

        assertEquals(AppLaunchStatus.INVALID_ARGUMENT, res.status)
        assertEquals("INVALID_ARGUMENT", res.errorCode)
    }

    @Test
    fun testNullContextSafeguardInAppLauncherImpl() {
        val launcher = AppLauncherImpl(null)
        val res = launcher.validateApp("Settings")

        assertEquals(AppLaunchStatus.UNAVAILABLE, res.status)
        assertEquals("NO_CONTEXT", res.errorCode)
    }

    @Test
    fun testGoalDispatcherAppLaunchIntegrationAndLock() {
        val apps = listOf(
            AppDescriptor("com.android.settings", "Settings", true, "com.android.settings.Settings")
        )
        val launcher = FakeAppLauncher(apps)
        val controller = ExecutionController()
        val dispatcher = GoalDispatcherImpl(controller, appLauncher = launcher)

        val details = dispatcher.dispatchAndProcessWithLock("open Settings")
        assertEquals("open Settings", details.command)
        assertEquals("OPEN_APP", details.operation)
        assertEquals("AppLauncherImpl", details.controllerName)
        assertEquals(com.agent.android.agent.skills.SkillStatus.SUCCESS, details.result.status)

        // Lock test: dispatch when already executing
        controller.acquireExecution()
        val rejectedDetails = dispatcher.dispatchAndProcessWithLock("open Settings")
        assertEquals("LOCK_REJECTED", rejectedDetails.operation)
        controller.releaseExecution()
    }
}
