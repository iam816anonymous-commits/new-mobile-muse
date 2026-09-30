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
    var evidenceReferences: List<String> = emptyList()
)
