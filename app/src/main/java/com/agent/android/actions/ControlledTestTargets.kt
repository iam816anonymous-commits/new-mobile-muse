package com.agent.android.actions

import com.agent.android.observation.GuidedTestApp

data class ControlTargetSpec(
    val displayName: String,
    val app: GuidedTestApp,
    val expectedPackage: String?,
    val defaultTargetQuery: String,
    val supportedActions: List<UiActionType>,
    val verificationStrategy: String
)

object ControlledTestTargets {

    val CALCULATOR = ControlTargetSpec(
        displayName = "Calculator",
        app = GuidedTestApp.CALCULATOR,
        expectedPackage = null, // Dynamic package resolution
        defaultTargetQuery = "1",
        supportedActions = listOf(UiActionType.CLICK),
        verificationStrategy = "VERIFY_DISPLAY_STATE_CHANGE"
    )

    val CHROME = ControlTargetSpec(
        displayName = "Chrome",
        app = GuidedTestApp.CHROME,
        expectedPackage = "com.android.chrome",
        defaultTargetQuery = "Search or type URL",
        supportedActions = listOf(UiActionType.CLICK, UiActionType.TEXT_INPUT, UiActionType.SCROLL_FORWARD),
        verificationStrategy = "VERIFY_URL_OR_TEXT_INPUT"
    )

    val SETTINGS = ControlTargetSpec(
        displayName = "Settings",
        app = GuidedTestApp.SETTINGS,
        expectedPackage = "com.android.settings",
        defaultTargetQuery = "Network & internet",
        supportedActions = listOf(UiActionType.CLICK, UiActionType.SCROLL_FORWARD),
        verificationStrategy = "VERIFY_SUBSCREEN_CHANGE"
    )

    val YOUTUBE = ControlTargetSpec(
        displayName = "YouTube",
        app = GuidedTestApp.YOUTUBE,
        expectedPackage = "com.google.android.youtube",
        defaultTargetQuery = "Search",
        supportedActions = listOf(UiActionType.CLICK, UiActionType.SCROLL_FORWARD),
        verificationStrategy = "VERIFY_SEARCH_CONTAINER"
    )

    fun getTargetForApp(app: GuidedTestApp): ControlTargetSpec {
        return when (app) {
            GuidedTestApp.CALCULATOR -> CALCULATOR
            GuidedTestApp.CHROME -> CHROME
            GuidedTestApp.SETTINGS -> SETTINGS
            GuidedTestApp.YOUTUBE -> YOUTUBE
        }
    }

    fun getAllTargets(): List<ControlTargetSpec> = listOf(CALCULATOR, CHROME, SETTINGS, YOUTUBE)
}
