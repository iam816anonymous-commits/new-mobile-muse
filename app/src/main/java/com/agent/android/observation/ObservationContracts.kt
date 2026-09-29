package com.agent.android.observation

data class InteractiveElement(
    val id: String,
    val text: String?,
    val contentDescription: String?,
    val className: String?,
    val isClickable: Boolean,
    val boundsInScreen: String
)

data class ScreenSnapshot(
    val packageName: String,
    val activityName: String?,
    val timestampMs: Long = System.currentTimeMillis(),
    val elementCount: Int
)

interface ScreenObserver {
    fun captureLightweightSnapshot(): ScreenSnapshot
    fun getInteractiveElementsSummary(): List<InteractiveElement>
}

data class InteractionIndicatorModel(
    val targetDescription: String,
    val actionType: String,
    val state: String,
    val result: String? = null,
    val bounds: String? = null
)
