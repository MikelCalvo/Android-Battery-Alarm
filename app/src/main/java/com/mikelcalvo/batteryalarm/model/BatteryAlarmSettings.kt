package com.mikelcalvo.batteryalarm.model

import android.net.Uri

data class BatteryAlarmSettings(
    var alarmType: AlarmType,
    var batteryPercentage: Int? = null,
    var notificationTone: Uri? = null,
    var repeat: Boolean = false,
    var repeatInterval: Long? = null
)

enum class AlarmType {
    BATTERY_FULL,
    BATTERY_LOW
}