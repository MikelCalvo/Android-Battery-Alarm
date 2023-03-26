package com.mikelcalvo.batteryalarm.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import com.mikelcalvo.batteryalarm.R
import com.mikelcalvo.batteryalarm.model.AlarmRepeatInterval
import com.mikelcalvo.batteryalarm.model.AlarmType
import com.mikelcalvo.batteryalarm.model.BatteryAlarmSettings

class BatteryLevelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BATTERY_CHANGED) {

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = level / scale.toFloat()

            if (batteryPct <= 0) {
                return
            }

            checkBatteryAlarms(context, batteryPct)
        }
    }

    private fun checkBatteryAlarms(context: Context, batteryPercentage: Float) {
        val sharedPreferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)

        for (alarmType in AlarmType.values()) {
            val isEnabled = sharedPreferences.getBoolean("enabled_${alarmType.name}", false)
            if (!isEnabled) continue

            val savedAlarmPercentage = sharedPreferences.getInt("percentage_${alarmType.name}", 15).toFloat()

            val shouldTriggerAlarm = when (alarmType) {
                AlarmType.BATTERY_FULL -> batteryPercentage >= savedAlarmPercentage
                AlarmType.BATTERY_LOW -> batteryPercentage <= savedAlarmPercentage
            }

            if (shouldTriggerAlarm) {
                val savedNotificationSound = sharedPreferences.getString("notificationSound_${alarmType.name}", "default")
                val savedRepeatInterval = sharedPreferences.getInt("repeatInterval_${alarmType.name}", AlarmRepeatInterval.NO_REPEAT.value)

                val alarm = BatteryAlarmSettings(
                    alarmType = alarmType,
                    batteryPercentage = savedAlarmPercentage.toInt(),
                    notificationTone = Uri.parse(savedNotificationSound ?: "default"),
                    repeatInterval = AlarmRepeatInterval.values().first { it.value == savedRepeatInterval }
                )

                startAlarmNotification(context, alarm)
            }
        }
    }

    private fun startAlarmNotification(context: Context, alarm: BatteryAlarmSettings) {
        val mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setOnCompletionListener {
                it.release()
            }
        }

        if (alarm.notificationTone.toString() == "default" || alarm.notificationTone == null) {
            val defaultUri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.default_alert)
            mediaPlayer.setDataSource(context, defaultUri)
        } else {
            mediaPlayer.setDataSource(context, alarm.notificationTone!!)
        }

        mediaPlayer.prepare()
        mediaPlayer.start()

        if(alarm.repeatInterval != AlarmRepeatInterval.NO_REPEAT) {
            mediaPlayer.setOnCompletionListener {
                it.release()
                mediaPlayer.prepare()
                mediaPlayer.start()

                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    mediaPlayer.release()
                }, alarm.repeatInterval.value * 1000L)
            }
        }
    }
}