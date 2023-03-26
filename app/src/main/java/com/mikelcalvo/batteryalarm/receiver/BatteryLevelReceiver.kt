package com.mikelcalvo.batteryalarm.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.BatteryManager
import android.util.Log
import com.mikelcalvo.batteryalarm.R
import com.mikelcalvo.batteryalarm.model.AlarmRepeatTimes
import com.mikelcalvo.batteryalarm.model.AlarmState
import com.mikelcalvo.batteryalarm.model.AlarmType
import com.mikelcalvo.batteryalarm.model.BatteryAlarmSettings
import kotlin.math.roundToInt


class BatteryLevelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == Intent.ACTION_BATTERY_CHANGED) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = (level / scale.toFloat() * 100).roundToInt()

            if (batteryPct <= 0) {
                return
            }

            checkBatteryAlarms(context, batteryPct)
        } else if (action == "com.mikelcalvo.batteryalarm.ACTION_STOP_FULL_BATTERY_ALARM") {
            stopBatteryAlarm(context, AlarmType.BATTERY_FULL)
        } else if (action == "com.mikelcalvo.batteryalarm.ACTION_STOP_LOW_BATTERY_ALARM") {
            stopBatteryAlarm(context, AlarmType.BATTERY_LOW)
        }
    }

    private fun checkBatteryAlarms(context: Context, batteryPercentage: Int) {
        val sharedPreferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)

        for (alarmType in AlarmType.values()) {
            val isEnabled = sharedPreferences.getBoolean("enabled_${alarmType.name}", false)
            if (!isEnabled) continue

            val savedAlarmPercentage = sharedPreferences.getInt("percentage_${alarmType.name}", 15)

            val shouldTriggerAlarm = when (alarmType) {
                AlarmType.BATTERY_FULL -> batteryPercentage >= 100
                AlarmType.BATTERY_LOW -> batteryPercentage <= savedAlarmPercentage
            }

            if (shouldTriggerAlarm) {
                val savedNotificationSound = sharedPreferences.getString("notificationSoundUri_${alarmType.name}", "default")
                val savedRepeatInterval = sharedPreferences.getInt("repeatTimes_${alarmType.name}", AlarmRepeatTimes.ONCE.value)

                val defaultAlarmSound = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.default_alarm)

                val selectedUri = if(savedNotificationSound == "default") {
                    defaultAlarmSound
                } else {
                    Uri.parse(savedNotificationSound)
                }

                val alarm = BatteryAlarmSettings(
                    alarmType = alarmType,
                    batteryPercentage = savedAlarmPercentage,
                    notificationTone = selectedUri,
                    repeatTimes = AlarmRepeatTimes.values().first { it.value == savedRepeatInterval }
                )

                startBatteryAlarm(context, alarm)
            }else {
                stopBatteryAlarm(context, alarmType)
            }
        }
    }

    private fun startBatteryAlarm(context: Context, alarm: BatteryAlarmSettings) {
        val intent = Intent(context, AlarmBroadcastReceiver::class.java)
        val alarmSoundString = if (alarm.notificationTone == null) {
            "default"
        } else {
            alarm.notificationTone.toString()
        }

        intent.putExtra("alarmSound", alarmSoundString)
        intent.putExtra("repeatingTimes", alarm.repeatTimes.value)
        intent.putExtra("alarmType", alarm.alarmType.name)
        val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, alarm.alarmType.ordinal, intent, flag)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent)
    }

    private fun stopBatteryAlarm(context: Context, alarmType: AlarmType) {
        val intent = Intent(context, AlarmBroadcastReceiver::class.java)
        val flag = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, alarmType.ordinal, intent, flag)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            if(alarmType == AlarmType.BATTERY_FULL) {
                AlarmState.shouldStopFullChargeAlarm.value = true
            }else{
                AlarmState.shouldStopLowBatteryAlarm.value = true
            }
        }
    }

    private fun isUriValid(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}