package com.mikelcalvo.batteryalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.mikelcalvo.batteryalarm.R
import com.mikelcalvo.batteryalarm.model.AlarmState
import com.mikelcalvo.batteryalarm.model.AlarmType

class AlarmBroadcastReceiver : BroadcastReceiver() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onReceive(context: Context, intent: Intent) {
        val defaultAlarmSound = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.default_alarm)
        val selectedAlarmSound = intent.getStringExtra("alarmSound")
        val repeatingTimes = intent.getIntExtra("repeatingTimes", 1)
        val alarmType = intent.getStringExtra("alarmType")

        if(defaultAlarmSound == null && selectedAlarmSound == null) {
            return
        }

        Log.d("AlarmBroadcastReceiver", "Alarm sound: $selectedAlarmSound")

        val alarmSound: Uri = Uri.parse(selectedAlarmSound ?: defaultAlarmSound.toString())

        if (isUriValid(context, alarmSound)) {
            var repeatedTimes = 0

            mediaPlayer = MediaPlayer.create(context, alarmSound).apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = false
                setOnCompletionListener {
                    val preferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
                    repeatedTimes++

                    if (repeatedTimes < repeatingTimes) {
                        if(alarmType == AlarmType.BATTERY_LOW.name && preferences.getBoolean("enabled_${AlarmType.BATTERY_LOW.name}", false)){
                            start()
                        }
                        if(alarmType == AlarmType.BATTERY_FULL.name && preferences.getBoolean("enabled_${AlarmType.BATTERY_FULL.name}", false)){
                            start()
                        }
                    }
                }
                start()
            }

            AlarmState.shouldStopLowBatteryAlarm.observeForever { shouldStop ->
                if(alarmType == AlarmType.BATTERY_LOW.name){
                    if (shouldStop) {
                        mediaPlayer?.stop()
                    }
                }
            }

            AlarmState.shouldStopFullChargeAlarm.observeForever { shouldStop ->
                if(alarmType == AlarmType.BATTERY_FULL.name){
                    if (shouldStop) {
                        mediaPlayer?.stop()
                    }
                }
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