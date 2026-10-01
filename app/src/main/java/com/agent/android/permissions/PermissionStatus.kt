package com.agent.android.permissions

enum class PermissionStatus {
    OBTAINED,
    NOT_GRANTED,
    REQUESTABLE,
    SETTINGS_REQUIRED,
    DEVICE_ADMIN_REQUIRED,
    SYSTEM_APP_REQUIRED,
    PRIVILEGED_ONLY,
    NOT_AVAILABLE_ON_API_27,
    NOT_APPLICABLE,
    UNKNOWN
}
