package com.agent.android.observation

import android.content.Context
import com.agent.android.test.model.TestCase
import com.agent.android.test.model.TestStatus

enum class ObservationTargetCategory {
    CURRENT_FOREGROUND_APP,
    SPECIFIC_PACKAGE,
    LOCALAGENT_APP,
    SYSTEM_UI,
    LAUNCHER,
    RECENTS,
    ACCESSIBILITY_ROOT_OF_CURRENT_WINDOW
}

data class ObservationTargetRequirement(
    val category: ObservationTargetCategory,
    val expectedPackage: String?,
    val expectedActivityKeyword: String? = null,
    val requiresExternalApp: Boolean = false,
    val isNegativeConditionTest: Boolean = false,
    val expectedNegativeState: ObservationState? = null
)

data class TargetValidationResult(
    val isValid: Boolean,
    val status: TestStatus,
    val targetCategory: ObservationTargetCategory,
    val expectedPackage: String?,
    val actualPackage: String?,
    val actualActivity: String?,
    val rootAvailable: Boolean,
    val nodeCount: Int,
    val failureReason: String?,
    val summaryText: String
)

class ObservationTargetResolver(private val context: Context? = null) {

    fun resolveTargetForTest(testCase: TestCase): ObservationTargetRequirement {
        val id = testCase.id
        return when {
            // Negative condition tests
            id == "P3.1-NEG-001" -> ObservationTargetRequirement(
                category = ObservationTargetCategory.SPECIFIC_PACKAGE,
                expectedPackage = testCase.expectedPackage ?: "com.android.chrome",
                isNegativeConditionTest = true,
                expectedNegativeState = ObservationState.TARGET_NOT_FOUND
            )
            id == "P3.1-NEG-005" || id == "P3.1-OBS-018" -> ObservationTargetRequirement(
                category = ObservationTargetCategory.CURRENT_FOREGROUND_APP,
                expectedPackage = null,
                isNegativeConditionTest = true,
                expectedNegativeState = ObservationState.ROOT_NODE_UNAVAILABLE
            )
            id == "P3.1-NEG-007" -> ObservationTargetRequirement(
                category = ObservationTargetCategory.SPECIFIC_PACKAGE,
                expectedPackage = "com.android.chrome",
                requiresExternalApp = true,
                isNegativeConditionTest = true,
                expectedNegativeState = ObservationState.TARGET_NOT_FOUND
            )
            id == "P3.1-NEG-008" -> ObservationTargetRequirement(
                category = ObservationTargetCategory.SPECIFIC_PACKAGE,
                expectedPackage = "com.android.chrome",
                requiresExternalApp = true,
                isNegativeConditionTest = true
            )
            id == "P3.1-OBS-019" -> ObservationTargetRequirement(
                category = ObservationTargetCategory.CURRENT_FOREGROUND_APP,
                expectedPackage = null,
                isNegativeConditionTest = true,
                expectedNegativeState = ObservationState.ACCESSIBILITY_DISABLED
            )

            // LocalAgent explicitly requested self-observation
            id == "P3.1-SYS-LOCAL-001" || id == "P3.1-OBS-028" -> ObservationTargetRequirement(
                category = ObservationTargetCategory.LOCALAGENT_APP,
                expectedPackage = "com.agent.android"
            )

            // Specific apps
            id.contains("CHROME") || id.contains("005") && id.startsWith("P3.1-XAPP") -> ObservationTargetRequirement(
                category = ObservationTargetCategory.SPECIFIC_PACKAGE,
                expectedPackage = "com.android.chrome",
                requiresExternalApp = true
            )
            id.contains("YOUTUBE") -> ObservationTargetRequirement(
                category = ObservationTargetCategory.SPECIFIC_PACKAGE,
                expectedPackage = "com.google.android.youtube",
                requiresExternalApp = true
            )
            id.contains("SETTINGS") || id.startsWith("P3.1-XAPP-001") || id.startsWith("P3.1-XAPP-002") -> ObservationTargetRequirement(
                category = ObservationTargetCategory.SPECIFIC_PACKAGE,
                expectedPackage = "com.android.settings",
                requiresExternalApp = true
            )
            id.contains("CALCULATOR") || id.startsWith("P3.1-XAPP-003") -> {
                val calcPkg = Phase3TestTargetController(context).resolveCalculatorPackage()
                ObservationTargetRequirement(
                    category = ObservationTargetCategory.SPECIFIC_PACKAGE,
                    expectedPackage = calcPkg ?: "com.google.android.calculator",
                    requiresExternalApp = true
                )
            }
            id.startsWith("P3.1-XAPP-004") -> ObservationTargetRequirement(
                category = ObservationTargetCategory.SPECIFIC_PACKAGE,
                expectedPackage = "com.google.android.deskclock",
                requiresExternalApp = true
            )

            // System surfaces
            id == "P3.1-SYS-SYSTEMUI-001" || id.contains("systemui") -> ObservationTargetRequirement(
                category = ObservationTargetCategory.SYSTEM_UI,
                expectedPackage = "com.android.systemui"
            )
            id == "P3.1-SYS-LAUNCHER-001" || id.contains("launcher") -> ObservationTargetRequirement(
                category = ObservationTargetCategory.LAUNCHER,
                expectedPackage = null
            )
            id == "P3.1-SYS-RECENTS-001" || id.contains("recents") -> ObservationTargetRequirement(
                category = ObservationTargetCategory.RECENTS,
                expectedPackage = null
            )

            // Default current window / foreground app
            else -> ObservationTargetRequirement(
                category = ObservationTargetCategory.CURRENT_FOREGROUND_APP,
                expectedPackage = testCase.expectedPackage ?: testCase.targetPackage
            )
        }
    }

    fun validateCapturedSnapshot(
        requirement: ObservationTargetRequirement,
        snapshot: ObservationSnapshot?
    ): TargetValidationResult {
        if (requirement.isNegativeConditionTest) {
            val rootIsNull = snapshot == null || snapshot.rootNode == null
            val stateMatches = requirement.expectedNegativeState != null && snapshot?.state == requirement.expectedNegativeState

            if (rootIsNull || stateMatches) {
                return TargetValidationResult(
                    isValid = true,
                    status = TestStatus.PASSED,
                    targetCategory = requirement.category,
                    expectedPackage = requirement.expectedPackage,
                    actualPackage = snapshot?.packageName,
                    actualActivity = snapshot?.activityName,
                    rootAvailable = snapshot?.rootNode != null,
                    nodeCount = snapshot?.nodeCount ?: 0,
                    failureReason = null,
                    summaryText = "Negative condition confirmed: ${requirement.expectedNegativeState ?: "rootNode == null"}"
                )
            } else {
                return TargetValidationResult(
                    isValid = false,
                    status = TestStatus.FAILED,
                    targetCategory = requirement.category,
                    expectedPackage = requirement.expectedPackage,
                    actualPackage = snapshot?.packageName,
                    actualActivity = snapshot?.activityName,
                    rootAvailable = snapshot?.rootNode != null,
                    nodeCount = snapshot?.nodeCount ?: 0,
                    failureReason = "UNEXPECTED_VALID_ROOT: Negative test expected null root/unavailable state but obtained valid root with ${snapshot?.nodeCount} nodes from package '${snapshot?.packageName}'",
                    summaryText = "UNEXPECTED_VALID_ROOT obtained (${snapshot?.nodeCount} nodes)"
                )
            }
        }

        if (snapshot == null) {
            return TargetValidationResult(
                isValid = false,
                status = TestStatus.BLOCKED,
                targetCategory = requirement.category,
                expectedPackage = requirement.expectedPackage,
                actualPackage = null,
                actualActivity = null,
                rootAvailable = false,
                nodeCount = 0,
                failureReason = "NO_SNAPSHOT: Observation Engine produced no snapshot",
                summaryText = "No snapshot available"
            )
        }

        val actualPkg = snapshot.packageName

        if (requirement.requiresExternalApp && actualPkg == "com.agent.android") {
            return TargetValidationResult(
                isValid = false,
                status = TestStatus.FAILED,
                targetCategory = requirement.category,
                expectedPackage = requirement.expectedPackage ?: "EXTERNAL_APP",
                actualPackage = actualPkg,
                actualActivity = snapshot.activityName,
                rootAvailable = snapshot.rootNode != null,
                nodeCount = snapshot.nodeCount,
                failureReason = "LOCALAGENT_FALLBACK_REJECTED: Test expected external app '${requirement.expectedPackage}' but captured LocalAgent (com.agent.android) as fallback",
                summaryText = "LocalAgent self-snapshot rejected for external target"
            )
        }

        return when (requirement.category) {
            ObservationTargetCategory.LOCALAGENT_APP -> {
                val match = actualPkg == "com.agent.android"
                TargetValidationResult(
                    isValid = match,
                    status = if (match) TestStatus.PASSED else TestStatus.FAILED,
                    targetCategory = requirement.category,
                    expectedPackage = "com.agent.android",
                    actualPackage = actualPkg,
                    actualActivity = snapshot.activityName,
                    rootAvailable = snapshot.rootNode != null,
                    nodeCount = snapshot.nodeCount,
                    failureReason = if (match) null else "Expected LocalAgent (com.agent.android) but observed '$actualPkg'",
                    summaryText = if (match) "LocalAgent self-observation verified (${snapshot.nodeCount} nodes)" else "Target mismatch"
                )
            }

            ObservationTargetCategory.SPECIFIC_PACKAGE -> {
                val expected = requirement.expectedPackage
                if (expected == null) {
                    TargetValidationResult(
                        isValid = false,
                        status = TestStatus.BLOCKED,
                        targetCategory = requirement.category,
                        expectedPackage = null,
                        actualPackage = actualPkg,
                        actualActivity = snapshot.activityName,
                        rootAvailable = snapshot.rootNode != null,
                        nodeCount = snapshot.nodeCount,
                        failureReason = "TARGET_UNRESOLVED: Requested target package is not installed or resolvable",
                        summaryText = "Target package not installed"
                    )
                } else if (actualPkg == expected) {
                    TargetValidationResult(
                        isValid = true,
                        status = TestStatus.PASSED,
                        targetCategory = requirement.category,
                        expectedPackage = expected,
                        actualPackage = actualPkg,
                        actualActivity = snapshot.activityName,
                        rootAvailable = snapshot.rootNode != null,
                        nodeCount = snapshot.nodeCount,
                        failureReason = null,
                        summaryText = "Target package '$expected' matched cleanly (${snapshot.nodeCount} nodes)"
                    )
                } else {
                    TargetValidationResult(
                        isValid = false,
                        status = TestStatus.FAILED,
                        targetCategory = requirement.category,
                        expectedPackage = expected,
                        actualPackage = actualPkg,
                        actualActivity = snapshot.activityName,
                        rootAvailable = snapshot.rootNode != null,
                        nodeCount = snapshot.nodeCount,
                        failureReason = "PACKAGE_MISMATCH: Expected target package '$expected' but observed '$actualPkg'",
                        summaryText = "Package mismatch: expected '$expected', observed '$actualPkg'"
                    )
                }
            }

            ObservationTargetCategory.SYSTEM_UI -> {
                val classification = WindowClassification.classify(actualPkg, snapshot.activityName)
                val isSysUi = classification == WindowClassification.SYSTEM_UI || actualPkg == "com.android.systemui"
                TargetValidationResult(
                    isValid = isSysUi,
                    status = if (isSysUi) TestStatus.PASSED else TestStatus.FAILED,
                    targetCategory = requirement.category,
                    expectedPackage = "com.android.systemui",
                    actualPackage = actualPkg,
                    actualActivity = snapshot.activityName,
                    rootAvailable = snapshot.rootNode != null,
                    nodeCount = snapshot.nodeCount,
                    failureReason = if (isSysUi) null else "Expected System UI but observed '$actualPkg'",
                    summaryText = if (isSysUi) "System UI surface confirmed (${snapshot.nodeCount} nodes)" else "Target mismatch"
                )
            }

            ObservationTargetCategory.LAUNCHER -> {
                val classification = WindowClassification.classify(actualPkg, snapshot.activityName)
                val isLauncher = classification == WindowClassification.LAUNCHER
                TargetValidationResult(
                    isValid = isLauncher,
                    status = if (isLauncher) TestStatus.PASSED else TestStatus.FAILED,
                    targetCategory = requirement.category,
                    expectedPackage = "LAUNCHER_SURFACE",
                    actualPackage = actualPkg,
                    actualActivity = snapshot.activityName,
                    rootAvailable = snapshot.rootNode != null,
                    nodeCount = snapshot.nodeCount,
                    failureReason = if (isLauncher) null else "Expected Launcher surface but observed '$actualPkg'",
                    summaryText = if (isLauncher) "Launcher surface confirmed (${snapshot.nodeCount} nodes)" else "Target mismatch"
                )
            }

            ObservationTargetCategory.RECENTS -> {
                val classification = WindowClassification.classify(actualPkg, snapshot.activityName)
                val isRecents = classification == WindowClassification.RECENTS
                TargetValidationResult(
                    isValid = isRecents,
                    status = if (isRecents) TestStatus.PASSED else TestStatus.FAILED,
                    targetCategory = requirement.category,
                    expectedPackage = "RECENTS_SURFACE",
                    actualPackage = actualPkg,
                    actualActivity = snapshot.activityName,
                    rootAvailable = snapshot.rootNode != null,
                    nodeCount = snapshot.nodeCount,
                    failureReason = if (isRecents) null else "Expected Recents surface but observed '$actualPkg'",
                    summaryText = if (isRecents) "Recents surface confirmed (${snapshot.nodeCount} nodes)" else "Target mismatch"
                )
            }

            else -> {
                TargetValidationResult(
                    isValid = snapshot.state == ObservationState.SUCCESS && snapshot.nodeCount > 0,
                    status = if (snapshot.state == ObservationState.SUCCESS) TestStatus.PASSED else TestStatus.FAILED,
                    targetCategory = requirement.category,
                    expectedPackage = requirement.expectedPackage ?: actualPkg,
                    actualPackage = actualPkg,
                    actualActivity = snapshot.activityName,
                    rootAvailable = snapshot.rootNode != null,
                    nodeCount = snapshot.nodeCount,
                    failureReason = if (snapshot.state == ObservationState.SUCCESS) null else "Observation state ${snapshot.state}",
                    summaryText = "Current window observed: $actualPkg (${snapshot.nodeCount} nodes)"
                )
            }
        }
    }
}
