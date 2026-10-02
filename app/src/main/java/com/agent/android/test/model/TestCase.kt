package com.agent.android.test.model

data class TestCase(
    val id: String,
    val commandId: String = "",
    val phase: String,
    val category: String,
    val name: String,
    val description: String,
    val command: String? = null,
    val expectedResult: String,
    val testType: TestType,
    val requiresPhysicalVerification: Boolean = false,
    val requiredPermission: String? = null,
    val requiredCapability: String? = null,
    val destructiveOrSafe: String = "SAFE",
    val timeout: Long = 6000L,
    var status: TestStatus = TestStatus.PENDING,
    var observedResult: String? = null,
    var error: String? = null,
    var timestamp: Long? = null,
    var duration: Long? = null,
    var evidenceReferences: List<String> = emptyList(),

    // Target App Observation fields for Phase 3.1
    val targetAppName: String? = null,
    val targetPackage: String? = null,
    val expectedPackage: String? = null,
    val launchIntentAction: String? = null,
    val observationTimeoutMs: Long = 10000L
)
