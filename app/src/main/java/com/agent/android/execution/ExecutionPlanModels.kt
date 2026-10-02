package com.agent.android.execution

import org.json.JSONArray
import org.json.JSONObject

enum class ExecutionPlanStatus {
    PLAN_CREATED,
    READY,
    RUNNING,
    WAITING,
    STEP_RUNNING,
    STEP_COMPLETED,
    STEP_FAILED,
    PAUSED,
    CANCELLED,
    COMPLETED,
    TIMED_OUT
}

enum class FailurePolicy {
    STOP_ON_FAILURE,
    CONTINUE_ON_FAILURE,
    RETRY,
    ABORT
}

data class ExecutionStep(
    val stepId: String,
    val commandId: String,
    val arguments: Map<String, String> = emptyMap(),
    val timeoutMs: Long = 60000L,
    val isWaitPrimitive: Boolean = false,
    val waitDurationMs: Long = 0L,
    val expectedResultPattern: String? = null
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("stepId", stepId)
        obj.put("commandId", commandId)
        val argsObj = JSONObject()
        for ((k, v) in arguments) {
            argsObj.put(k, v)
        }
        obj.put("arguments", argsObj)
        obj.put("timeoutMs", timeoutMs)
        obj.put("isWaitPrimitive", isWaitPrimitive)
        obj.put("waitDurationMs", waitDurationMs)
        obj.put("expectedResultPattern", expectedResultPattern ?: JSONObject.NULL)
        return obj
    }

    companion object {
        fun createWaitStep(stepId: String, durationMs: Long): ExecutionStep {
            return ExecutionStep(
                stepId = stepId,
                commandId = "wait",
                arguments = mapOf("durationMs" to durationMs.toString()),
                timeoutMs = durationMs + 5000L,
                isWaitPrimitive = true,
                waitDurationMs = durationMs
            )
        }

        fun fromJsonObject(json: JSONObject): ExecutionStep {
            val argsMap = mutableMapOf<String, String>()
            val argsObj = json.optJSONObject("arguments")
            if (argsObj != null) {
                val keys = argsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    argsMap[k] = argsObj.optString(k, "")
                }
            }
            return ExecutionStep(
                stepId = json.optString("stepId", ""),
                commandId = json.optString("commandId", ""),
                arguments = argsMap,
                timeoutMs = json.optLong("timeoutMs", 60000L),
                isWaitPrimitive = json.optBoolean("isWaitPrimitive", false),
                waitDurationMs = json.optLong("waitDurationMs", 0L),
                expectedResultPattern = if (json.isNull("expectedResultPattern")) null else json.optString("expectedResultPattern")
            )
        }
    }
}

data class ExecutionPlan(
    val planId: String,
    val goalDescription: String,
    val steps: List<ExecutionStep>,
    val createdAtMs: Long = System.currentTimeMillis(),
    val failurePolicy: FailurePolicy = FailurePolicy.STOP_ON_FAILURE,
    val timeoutPolicyMs: Long = 300000L,
    val currentStepIndex: Int = 0,
    val status: ExecutionPlanStatus = ExecutionPlanStatus.PLAN_CREATED
) {
    fun toJsonString(): String {
        val obj = JSONObject()
        obj.put("planId", planId)
        obj.put("goalDescription", goalDescription)
        obj.put("createdAtMs", createdAtMs)
        obj.put("failurePolicy", failurePolicy.name)
        obj.put("timeoutPolicyMs", timeoutPolicyMs)
        obj.put("currentStepIndex", currentStepIndex)
        obj.put("status", status.name)

        val stepsArray = JSONArray()
        for (step in steps) {
            stepsArray.put(step.toJsonObject())
        }
        obj.put("steps", stepsArray)
        return obj.toString(2)
    }

    companion object {
        fun fromJsonString(jsonStr: String): ExecutionPlan {
            val json = JSONObject(jsonStr)
            val stepsList = mutableListOf<ExecutionStep>()
            val stepsArray = json.optJSONArray("steps")
            if (stepsArray != null) {
                for (i in 0 until stepsArray.length()) {
                    val stepObj = stepsArray.optJSONObject(i)
                    if (stepObj != null) {
                        stepsList.add(ExecutionStep.fromJsonObject(stepObj))
                    }
                }
            }

            val status = try {
                ExecutionPlanStatus.valueOf(json.optString("status", ExecutionPlanStatus.PLAN_CREATED.name))
            } catch (e: Exception) {
                ExecutionPlanStatus.PLAN_CREATED
            }

            val failPolicy = try {
                FailurePolicy.valueOf(json.optString("failurePolicy", FailurePolicy.STOP_ON_FAILURE.name))
            } catch (e: Exception) {
                FailurePolicy.STOP_ON_FAILURE
            }

            return ExecutionPlan(
                planId = json.optString("planId", ""),
                goalDescription = json.optString("goalDescription", ""),
                steps = stepsList,
                createdAtMs = json.optLong("createdAtMs", System.currentTimeMillis()),
                failurePolicy = failPolicy,
                timeoutPolicyMs = json.optLong("timeoutPolicyMs", 300000L),
                currentStepIndex = json.optInt("currentStepIndex", 0),
                status = status
            )
        }
    }
}
