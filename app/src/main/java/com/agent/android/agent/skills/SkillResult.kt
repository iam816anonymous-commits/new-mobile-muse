package com.agent.android.agent.skills

enum class SkillStatus {
    SUCCESS,
    FAILED,
    PERMISSION_REQUIRED,
    UNSUPPORTED,
    UNAVAILABLE,
    CANCELLED,
    INVALID_GOAL
}

data class SkillResult(
    val operation: String,
    val status: SkillStatus,
    val message: String,
    val durationMs: Long,
    val errorCode: String? = null
)
