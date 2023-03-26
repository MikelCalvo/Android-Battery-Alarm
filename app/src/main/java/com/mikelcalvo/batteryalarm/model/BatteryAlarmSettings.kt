package com.mikelcalvo.batteryalarm.model

import android.net.Uri

data class BatteryAlarmSettings(
    var alarmType: AlarmType,
    var batteryPercentage: Int? = 15,
    var notificationTone: Uri? = null,
    var repeatInterval: AlarmRepeatInterval = AlarmRepeatInterval.NO_REPEAT
)

enum class AlarmType {
    BATTERY_FULL,
    BATTERY_LOW
}

enum class AlarmRepeatInterval(val value: Int) {
    NO_REPEAT(0),
    TEN_SECONDS(10),
    THIRTY_SECONDS(30),
    ONE_MINUTE(60),
    INFINITE(Int.MAX_VALUE)
}