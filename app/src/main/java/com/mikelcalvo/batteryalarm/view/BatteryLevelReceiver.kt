package com.mikelcalvo.batteryalarm.view

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mikelcalvo.batteryalarm.R
import com.mikelcalvo.batteryalarm.model.AlarmRepeatInterval
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

                val savedNotificationSound = sharedPreferences.getString("notificationSound_${alarmType.name}", "default")
                val savedRepeatInterval = sharedPreferences.getInt("repeatInterval_${alarmType.name}", AlarmRepeatInterval.NO_REPEAT.value)

                val alarm = BatteryAlarmSettings(
                    alarmType = alarmType,
                    batteryPercentage = savedAlarmPercentage,
                    notificationTone = Uri.parse(savedNotificationSound ?: "default"),
                    repeatInterval = AlarmRepeatInterval.values().first { it.value == savedRepeatInterval }
                )

                startAlarmNotification(context, alarm)
            }
        }
    }

    private fun startAlarmNotification(context: Context, alarm: BatteryAlarmSettings) {
        val channelId = "battery_alarm_notification_${alarm.alarmType}"
        val notificationId = alarm.alarmType.ordinal

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationSound: Uri = if (alarm.notificationTone == null || alarm.notificationTone.toString() == "default") {
            Uri.parse("android.resource://" + context.packageName + "/" + R.raw.default_alert)
        } else {
            alarm.notificationTone!!
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(
                if (alarm.alarmType == AlarmType.BATTERY_FULL) context.getString(R.string.battery_full)
                else context.getString(R.string.battery_low)
            )
            .setSmallIcon(
                if(alarm.alarmType == AlarmType.BATTERY_FULL) R.drawable.ic_battery_full
                else R.drawable.ic_battery_low
            )
            .setContentIntent(pendingIntent)
            .setSound(notificationSound)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(alarm.repeatInterval == AlarmRepeatInterval.NO_REPEAT)

        val notificationManager = NotificationManagerCompat.from(context)
        createNotificationChannel(context, channelId)

        try {
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }


        if (alarm.repeatInterval != AlarmRepeatInterval.NO_REPEAT) {
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                try {
                    notificationManager.notify(notificationId, notificationBuilder.build())
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }, alarm.repeatInterval.value * 1000L)
        }
    }

    private fun createNotificationChannel(context: Context, channelId: String) {
        val name = context.getString(R.string.app_name)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, name, importance)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}