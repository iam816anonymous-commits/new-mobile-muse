package com.agent.android.actions

interface Action {
    val id: String
    val actionType: String
    val description: String
}

data class ActionResult(
    val actionId: String,
    val success: Boolean,
    val message: String,
    val timestampMs: Long = System.currentTimeMillis()
)

class ActionController {
    @Volatile
    private var isActionExecuting: Boolean = false

    @Volatile
    private var currentActionId: String? = null

    fun canExecuteAction(): Boolean = !isActionExecuting

    @Synchronized
    fun executeAction(action: Action, block: (Action) -> ActionResult): ActionResult {
        if (isActionExecuting) {
            return ActionResult(
                actionId = action.id,
                success = false,
                message = "Rejected: Another action is currently executing. Constraint: ONE ACTION AT A TIME."
            )
        }
        isActionExecuting = true
        currentActionId = action.id
        return try {
            block(action)
        } finally {
            isActionExecuting = false
            currentActionId = null
        }
    }

    fun isExecuting(): Boolean = isActionExecuting
    fun getCurrentActionId(): String? = currentActionId
}
