package com.mikelcalvo.batteryalarm.model

import android.net.Uri

data class BatteryAlarmSettings(
    var alarmType: AlarmType,
    var batteryPercentage: Int? = 15,
    var notificationTone: Uri? = null,
    var repeatTimes: AlarmRepeatTimes = AlarmRepeatTimes.ONCE
)

enum class AlarmType {
    BATTERY_FULL,
    BATTERY_LOW
}

enum class AlarmRepeatTimes(val value: Int) {
    ONCE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    INFINITE(Int.MAX_VALUE)
}