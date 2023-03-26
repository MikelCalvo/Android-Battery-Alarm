package com.mikelcalvo.batteryalarm.view

import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import com.mikelcalvo.batteryalarm.R
import com.mikelcalvo.batteryalarm.model.AlarmRepeatInterval
import com.mikelcalvo.batteryalarm.model.AlarmType
import com.mikelcalvo.batteryalarm.model.BatteryAlarmSettings
import com.mikelcalvo.batteryalarm.model.NotificationSound
import java.util.concurrent.TimeUnit

class BatteryAlarmAdapter(private val alarms: List<BatteryAlarmSettings>) :
    RecyclerView.Adapter<BatteryAlarmAdapter.ViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val ivBatteryIcon: ImageView = view.findViewById(R.id.ivBatteryIcon)
        val tvBatteryTitle: TextView = view.findViewById(R.id.tvBatteryTitle)
        val switchAlarm: Switch = view.findViewById(R.id.switchAlarm)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_battery_alarm, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return alarms.size.coerceAtMost(2)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = alarms[position]
        when (alarm.alarmType) {
            AlarmType.BATTERY_FULL -> {
                holder.ivBatteryIcon.setImageResource(R.drawable.ic_battery_full)
                holder.tvBatteryTitle.setText(R.string.battery_full)
            }
            AlarmType.BATTERY_LOW -> {
                holder.ivBatteryIcon.setImageResource(R.drawable.ic_battery_low)
                holder.tvBatteryTitle.setText(R.string.battery_low)
            }
        }

        holder.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            val context = holder.view.context

            val editor = context.getSharedPreferences("alarms", Context.MODE_PRIVATE).edit()
            editor.putBoolean("enabled_${alarm.alarmType.name}", isChecked)
            editor.apply()

        }

        val sharedPreferences = holder.view.context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
        val isEnabled = sharedPreferences.getBoolean("enabled_${alarm.alarmType.name}", false)
        holder.switchAlarm.isChecked = isEnabled

        holder.view.setOnClickListener {
            showSettingsDialog(it.context, alarm)
        }
    }

    private fun showSettingsDialog(context: Context, alarm: BatteryAlarmSettings) {
        val sharedPreferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_battery_alarm_settings)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setDimAmount(0.8f)
        dialog.setCanceledOnTouchOutside(false)

        val notificationSoundTextView = dialog.findViewById<TextView>(R.id.notificationSound)
        val savedNotificationSound = sharedPreferences.getString("notificationSound_${alarm.alarmType.name}", context.getString(R.string.default_tone))
        notificationSoundTextView.text = savedNotificationSound
        notificationSoundTextView.setOnClickListener {
            showNotificationSoundsDialog(context, notificationSoundTextView)
        }

        val cancelRepeat = dialog.findViewById<TextView>(R.id.cancelRepeat)
        val repeat10s = dialog.findViewById<TextView>(R.id.repeat10s)
        val repeat30s = dialog.findViewById<TextView>(R.id.repeat30s)
        val repeat1m = dialog.findViewById<TextView>(R.id.repeat1m)
        val repeatInfinite = dialog.findViewById<TextView>(R.id.repeatInfinite)

        with(cancelRepeat) {
            setOnClickListener { selectRepeatOption(context, cancelRepeat, repeat10s, repeat30s, repeat1m, repeatInfinite) }
        }
        with(repeat10s) {
            setOnClickListener { selectRepeatOption(context, repeat10s, cancelRepeat, repeat30s, repeat1m, repeatInfinite) }
        }
        with(repeat30s) {
            setOnClickListener { selectRepeatOption(context, repeat30s, cancelRepeat, repeat10s, repeat1m, repeatInfinite) }
        }
        with(repeat1m) {
            setOnClickListener { selectRepeatOption(context, repeat1m, cancelRepeat, repeat10s, repeat30s, repeatInfinite) }
        }
        with(repeatInfinite) {
            setOnClickListener { selectRepeatOption(context, repeatInfinite, cancelRepeat, repeat10s, repeat30s, repeat1m) }
        }

        when (sharedPreferences.getInt("repeat_${alarm.alarmType.name}", AlarmRepeatInterval.NO_REPEAT.value)) {
            AlarmRepeatInterval.NO_REPEAT.value -> selectRepeatOption(context, cancelRepeat, repeat10s, repeat30s, repeat1m, repeatInfinite)
            AlarmRepeatInterval.TEN_SECONDS.value -> selectRepeatOption(context, repeat10s, cancelRepeat, repeat30s, repeat1m, repeatInfinite)
            AlarmRepeatInterval.THIRTY_SECONDS.value -> selectRepeatOption(context, repeat30s, cancelRepeat, repeat10s, repeat1m, repeatInfinite)
            AlarmRepeatInterval.ONE_MINUTE.value -> selectRepeatOption(context, repeat1m, cancelRepeat, repeat10s, repeat30s, repeatInfinite)
            AlarmRepeatInterval.INFINITE.value -> selectRepeatOption(context, repeatInfinite, cancelRepeat, repeat10s, repeat30s, repeat1m)
        }

        val lowBatterySettingsCard = dialog.findViewById<CardView>(R.id.lowBatterySettingsCard)

        val lowBatteryPercentage = dialog.findViewById<Slider>(R.id.lowBatteryPercentage)
        val savedPercentage = sharedPreferences.getInt("percentage_${alarm.alarmType.name}", 15)
        lowBatteryPercentage.value = savedPercentage.toFloat()

        lowBatterySettingsCard.visibility = if (alarm.alarmType == AlarmType.BATTERY_LOW) {
            View.VISIBLE
        } else {
            View.GONE
        }

        val saveButton = dialog.findViewById<Button>(R.id.saveButton)

        saveButton.setOnClickListener {
            val editor = sharedPreferences.edit()

            editor.putString("notificationSound_${alarm.alarmType.name}", notificationSoundTextView.text.toString())
            editor.putInt("repeat_${alarm.alarmType.name}", getSelectedRepeatOption(cancelRepeat, repeat10s, repeat30s, repeat1m, repeatInfinite))

            if(alarm.alarmType == AlarmType.BATTERY_LOW) {
                editor.putInt("percentage_${alarm.alarmType.name}", getSelectedBatteryPercentage(dialog.findViewById(R.id.lowBatteryPercentage)))
            }

            editor.apply()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showNotificationSoundsDialog(context: Context, notificationSoundTextView: TextView) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_notification_sounds)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setDimAmount(0.8f)
        dialog.setCanceledOnTouchOutside(false)

        val notificationSoundsList: RecyclerView = dialog.findViewById(R.id.notificationSoundsList)
        notificationSoundsList.layoutManager = LinearLayoutManager(context)

        val notificationSounds = getNotificationSounds(context)

        val notificationSoundAdapter = NotificationSoundAdapter(context, notificationSounds, { uri ->
            playNotificationSound(context, uri)
        }) { selectedNotificationSound ->
            notificationSoundTextView.text = selectedNotificationSound.title
            dialog.dismiss()
        }
        notificationSoundsList.adapter = notificationSoundAdapter

        dialog.show()
    }

    private fun getNotificationSounds(context: Context): List<NotificationSound> {
        val ringtoneManager = RingtoneManager(context).apply {
            setType(RingtoneManager.TYPE_NOTIFICATION)
        }
        val cursor = ringtoneManager.cursor
        val notificationSounds = mutableListOf<NotificationSound>()

        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uriString = cursor.getString(RingtoneManager.URI_COLUMN_INDEX) + "/" + cursor.getString(RingtoneManager.ID_COLUMN_INDEX)
            val uri = Uri.parse(uriString)
            notificationSounds.add(NotificationSound(title, uri))
        }

        return notificationSounds
    }

    private fun playNotificationSound(context: Context, uri: Uri) {
        mediaPlayer?.stop()
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, uri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setOnPreparedListener {
                it.start()
            }
            setOnCompletionListener {
                it.release()
                mediaPlayer = null
            }
            prepareAsync()
        }
    }

    private fun selectRepeatOption(
        context: Context,
        selectedOption: TextView,
        vararg otherOptions: TextView
    ) {
        selectedOption.backgroundTintList = ContextCompat.getColorStateList(context, R.color.colorSecondary)

        for (option in otherOptions) {
            option.backgroundTintList = ContextCompat.getColorStateList(context, R.color.colorBackground)
        }
    }

    private fun getSelectedRepeatOption(
        cancelRepeat: TextView,
        repeat10s: TextView,
        repeat30s: TextView,
        repeat1m: TextView,
        repeatInfinite: TextView
    ): Int {
        return when {
            cancelRepeat.backgroundTintList == ContextCompat.getColorStateList(cancelRepeat.context, R.color.colorSecondary) -> 0
            repeat10s.backgroundTintList == ContextCompat.getColorStateList(repeat10s.context, R.color.colorSecondary) -> 10
            repeat30s.backgroundTintList == ContextCompat.getColorStateList(repeat30s.context, R.color.colorSecondary) -> 30
            repeat1m.backgroundTintList == ContextCompat.getColorStateList(repeat1m.context, R.color.colorSecondary) -> 60
            repeatInfinite.backgroundTintList == ContextCompat.getColorStateList(repeatInfinite.context, R.color.colorSecondary) -> Int.MAX_VALUE
            else -> 0
        }
    }

    private fun getSelectedBatteryPercentage(batteryPercentage: Slider): Int {
        return batteryPercentage.value.toInt()
    }
}