package com.mikelcalvo.batteryalarm.model

import androidx.lifecycle.MutableLiveData

object AlarmState {
    val shouldStopFullChargeAlarm = MutableLiveData(true)
    val shouldStopLowBatteryAlarm = MutableLiveData(true)
}
