package com.agent.android.observation

import android.content.Context
import android.content.pm.PackageManager
import com.agent.android.agent.skills.app.AppLauncherImpl

enum class ObservationTargetType {
    LOCALAGENT,
    ANDROID_SETTINGS,
    CHROME,
    YOUTUBE,
    CALCULATOR,
    SYSTEM_UI,
    RECENTS,
    LAUNCHER,
    EXTERNAL_APPLICATION,
    NO_ACTIVE_WINDOW,
    CUSTOM_TARGET
}

data class TestTargetSpec(
    val targetType: ObservationTargetType,
    val expectedPackage: String?,
    val expectedActivityKeyword: String? = null
)

data class TargetPreparationResult(
    val success: Boolean,
    val targetType: ObservationTargetType,
    val expectedPackage: String?,
    val actualPackage: String?,
    val actualActivity: String?,
    val statusText: String,
    val errorReason: String? = null
)

class Phase3TestTargetController(private val context: Context?) {

    fun resolveTargetSpec(testId: String): TestTargetSpec {
        return when {
            testId.contains("LOCAL") || testId.contains("028") || (testId.contains("001") && testId.startsWith("P3.1-SYS")) -> {
                TestTargetSpec(ObservationTargetType.LOCALAGENT, "com.agent.android")
            }
            testId.contains("CHROME") || testId.contains("browser") -> {
                TestTargetSpec(ObservationTargetType.CHROME, "com.android.chrome")
            }
            testId.contains("YOUTUBE") -> {
                TestTargetSpec(ObservationTargetType.YOUTUBE, "com.google.android.youtube")
            }
            testId.contains("SETTINGS") || testId.contains("settings") -> {
                TestTargetSpec(ObservationTargetType.ANDROID_SETTINGS, "com.android.settings")
            }
            testId.contains("CALCULATOR") || testId.contains("calculator") -> {
                val calcPkg = resolveCalculatorPackage()
                TestTargetSpec(ObservationTargetType.CALCULATOR, calcPkg)
            }
            testId.contains("LAUNCHER") || testId.contains("launcher") -> {
                TestTargetSpec(ObservationTargetType.LAUNCHER, null)
            }
            testId.contains("RECENTS") || testId.contains("recents") -> {
                TestTargetSpec(ObservationTargetType.RECENTS, null)
            }
            testId.contains("SYSTEMUI") || testId.contains("systemui") -> {
                TestTargetSpec(ObservationTargetType.SYSTEM_UI, "com.android.systemui")
            }
            else -> {
                TestTargetSpec(ObservationTargetType.LOCALAGENT, "com.agent.android")
            }
        }
    }

    fun resolveCalculatorPackage(): String? {
        if (context == null) return null
        val pm = context.packageManager
        val appLauncher = AppLauncherImpl(context)
        val valRes = appLauncher.validateApp("Calculator")
        if (valRes.status.name.contains("RESOLVED") && valRes.resolvedPackage != null) {
            return valRes.resolvedPackage
        }
        val installed = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emptyList()
        }
        for (info in installed) {
            if (info.packageName.contains("calculator", ignoreCase = true) || info.packageName.contains("calc", ignoreCase = true)) {
                if (pm.getLaunchIntentForPackage(info.packageName) != null) {
                    return info.packageName
                }
            }
        }
        return null
    }

    fun verifyTargetMatch(spec: TestTargetSpec, actualPackage: String?, actualActivity: String?): TargetPreparationResult {
        if (actualPackage == null || actualPackage.isEmpty()) {
            return TargetPreparationResult(
                success = false,
                targetType = spec.targetType,
                expectedPackage = spec.expectedPackage,
                actualPackage = actualPackage,
                actualActivity = actualActivity,
                statusText = "NO_FOREGROUND_PACKAGE",
                errorReason = "Foreground package is null or empty"
            )
        }

        return when (spec.targetType) {
            ObservationTargetType.LOCALAGENT -> {
                val match = actualPackage == "com.agent.android"
                TargetPreparationResult(
                    success = match,
                    targetType = spec.targetType,
                    expectedPackage = "com.agent.android",
                    actualPackage = actualPackage,
                    actualActivity = actualActivity,
                    statusText = if (match) "LOCALAGENT_CONFIRMED" else "WRONG_TARGET",
                    errorReason = if (match) null else "Expected LocalAgent (com.agent.android) but detected $actualPackage"
                )
            }
            ObservationTargetType.ANDROID_SETTINGS,
            ObservationTargetType.CHROME,
            ObservationTargetType.YOUTUBE,
            ObservationTargetType.CALCULATOR,
            ObservationTargetType.EXTERNAL_APPLICATION -> {
                val expectedPkg = spec.expectedPackage
                if (expectedPkg == null) {
                    TargetPreparationResult(
                        success = false,
                        targetType = spec.targetType,
                        expectedPackage = null,
                        actualPackage = actualPackage,
                        actualActivity = actualActivity,
                        statusText = "TARGET_PACKAGE_UNRESOLVED",
                        errorReason = "Required target application is not installed/resolvable"
                    )
                } else if (actualPackage == expectedPkg) {
                    TargetPreparationResult(
                        success = true,
                        targetType = spec.targetType,
                        expectedPackage = expectedPkg,
                        actualPackage = actualPackage,
                        actualActivity = actualActivity,
                        statusText = "TARGET_CONFIRMED"
                    )
                } else {
                    TargetPreparationResult(
                        success = false,
                        targetType = spec.targetType,
                        expectedPackage = expectedPkg,
                        actualPackage = actualPackage,
                        actualActivity = actualActivity,
                        statusText = "WRONG_TARGET",
                        errorReason = "Expected target package $expectedPkg but detected $actualPackage"
                    )
                }
            }
            ObservationTargetType.SYSTEM_UI -> {
                val classification = WindowClassification.classify(actualPackage, actualActivity)
                val isSysUi = classification == WindowClassification.SYSTEM_UI || actualPackage == "com.android.systemui"
                TargetPreparationResult(
                    success = isSysUi,
                    targetType = spec.targetType,
                    expectedPackage = "com.android.systemui",
                    actualPackage = actualPackage,
                    actualActivity = actualActivity,
                    statusText = if (isSysUi) "SYSTEM_UI_CONFIRMED" else "WRONG_TARGET",
                    errorReason = if (isSysUi) null else "Expected System UI but detected $actualPackage"
                )
            }
            ObservationTargetType.RECENTS -> {
                val classification = WindowClassification.classify(actualPackage, actualActivity)
                val isRecents = classification == WindowClassification.RECENTS
                TargetPreparationResult(
                    success = isRecents,
                    targetType = spec.targetType,
                    expectedPackage = "RECENTS_SURFACE",
                    actualPackage = actualPackage,
                    actualActivity = actualActivity,
                    statusText = if (isRecents) "RECENTS_CONFIRMED" else "WRONG_TARGET",
                    errorReason = if (isRecents) null else "Expected Recents surface but detected $actualPackage"
                )
            }
            ObservationTargetType.LAUNCHER -> {
                val classification = WindowClassification.classify(actualPackage, actualActivity)
                val isLauncher = classification == WindowClassification.LAUNCHER
                TargetPreparationResult(
                    success = isLauncher,
                    targetType = spec.targetType,
                    expectedPackage = "LAUNCHER_SURFACE",
                    actualPackage = actualPackage,
                    actualActivity = actualActivity,
                    statusText = if (isLauncher) "LAUNCHER_CONFIRMED" else "WRONG_TARGET",
                    errorReason = if (isLauncher) null else "Expected Home/Launcher surface but detected $actualPackage"
                )
            }
            else -> {
                TargetPreparationResult(
                    success = true,
                    targetType = spec.targetType,
                    expectedPackage = spec.expectedPackage,
                    actualPackage = actualPackage,
                    actualActivity = actualActivity,
                    statusText = "CUSTOM_TARGET_CONFIRMED"
                )
            }
        }
    }
}
