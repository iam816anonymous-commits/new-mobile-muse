package com.agent.android.test.storage

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.agent.android.test.FoundationTestRegistry
import com.agent.android.test.model.TestStatus
import org.json.JSONArray
import org.json.JSONObject

class TestResultStore(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "localagent_test_results"
        private const val KEY_RESULTS_JSON = "key_results_json"
        private const val KEY_LAST_RUN_TIMESTAMP = "key_last_run_timestamp"
        private const val KEY_DEVICE_INFO = "key_device_info"
        private const val KEY_BUILD_VERSION = "key_build_version"
    }

    fun saveResults(registry: FoundationTestRegistry) {
        val testCases = registry.getAllTestCases()
        val jsonArray = JSONArray()

        for (tc in testCases) {
            val obj = JSONObject()
            obj.put("id", tc.id)
            obj.put("status", tc.status.name)
            obj.put("observedResult", tc.observedResult ?: "")
            obj.put("error", tc.error ?: "")
            obj.put("duration", tc.duration ?: 0L)
            obj.put("timestamp", tc.timestamp ?: 0L)

            val evArray = JSONArray()
            tc.evidenceReferences.forEach { evArray.put(it) }
            obj.put("evidence", evArray)

            jsonArray.put(obj)
        }

        val buildVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }

        prefs.edit()
            .putString(KEY_RESULTS_JSON, jsonArray.toString())
            .putLong(KEY_LAST_RUN_TIMESTAMP, System.currentTimeMillis())
            .putString(KEY_DEVICE_INFO, "${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})")
            .putString(KEY_BUILD_VERSION, buildVersion)
            .apply()
    }

    fun loadResults(registry: FoundationTestRegistry): Boolean {
        val jsonStr = prefs.getString(KEY_RESULTS_JSON, null) ?: return false
        return try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id")
                val statusStr = obj.optString("status")
                val observed = obj.optString("observedResult").takeIf { it.isNotEmpty() }
                val err = obj.optString("error").takeIf { it.isNotEmpty() }
                val dur = obj.optLong("duration", 0L)

                val evArray = obj.optJSONArray("evidence")
                val evidenceList = mutableListOf<String>()
                if (evArray != null) {
                    for (j in 0 until evArray.length()) {
                        evidenceList.add(evArray.getString(j))
                    }
                }

                val status = try {
                    TestStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    TestStatus.PENDING
                }

                registry.updateTestCase(
                    id = id,
                    status = status,
                    observedResult = observed,
                    error = err,
                    duration = dur,
                    evidenceReferences = evidenceList
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearAllResults(registry: FoundationTestRegistry) {
        registry.clearAllResults()
        prefs.edit().clear().apply()
    }

    fun getLastRunTimestamp(): Long = prefs.getLong(KEY_LAST_RUN_TIMESTAMP, 0L)
    fun getDeviceInfo(): String = prefs.getString(KEY_DEVICE_INFO, "UNKNOWN") ?: "UNKNOWN"
    fun getBuildVersion(): String = prefs.getString(KEY_BUILD_VERSION, "UNKNOWN") ?: "UNKNOWN"
}
