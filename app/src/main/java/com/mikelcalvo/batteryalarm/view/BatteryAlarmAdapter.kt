package com.mikelcalvo.batteryalarm.view

import android.app.Dialog
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
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import com.mikelcalvo.batteryalarm.R
import com.mikelcalvo.batteryalarm.model.*

class BatteryAlarmAdapter(private val alarms: List<BatteryAlarmSettings>) :
    RecyclerView.Adapter<BatteryAlarmAdapter.ViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var selectedNotificationSound: NotificationSound? = null

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

            if(alarm.alarmType == AlarmType.BATTERY_LOW && !isChecked) {
                val intent = Intent("com.mikelcalvo.batteryalarm.ACTION_STOP_LOW_BATTERY_ALARM")
                holder.view.context.sendBroadcast(intent)
            }

            if(alarm.alarmType == AlarmType.BATTERY_FULL && !isChecked) {
                val intent = Intent("com.mikelcalvo.batteryalarm.ACTION_STOP_FULL_BATTERY_ALARM")
                holder.view.context.sendBroadcast(intent)
            }
        }

        val sharedPreferences = holder.view.context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
        val isEnabled = sharedPreferences.getBoolean("enabled_${alarm.alarmType.name}", false)
        holder.switchAlarm.isChecked = isEnabled

        if(alarm.alarmType == AlarmType.BATTERY_LOW && !isEnabled) {
            val intent = Intent("com.mikelcalvo.batteryalarm.ACTION_STOP_LOW_BATTERY_ALARM")
            holder.view.context.sendBroadcast(intent)
        }

        if(alarm.alarmType == AlarmType.BATTERY_FULL && !isEnabled) {
            val intent = Intent("com.mikelcalvo.batteryalarm.ACTION_STOP_FULL_BATTERY_ALARM")
            holder.view.context.sendBroadcast(intent)
        }

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
        val selectedNotificationSoundTitle = sharedPreferences.getString("notificationSound_${alarm.alarmType.name}", context.getString(R.string.default_tone))
        val selectedNotificationSoundUri = sharedPreferences.getString("notificationSoundUri_${alarm.alarmType.name}", null)

        selectedNotificationSound = if(selectedNotificationSoundUri != null) {
            NotificationSound(selectedNotificationSoundTitle!!, Uri.parse(selectedNotificationSoundUri))
        } else {
            NotificationSound(selectedNotificationSoundTitle!!, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }
        notificationSoundTextView.text = selectedNotificationSoundTitle
        notificationSoundTextView.setOnClickListener {
            showNotificationSoundsDialog(context, notificationSoundTextView)
        }

        val repeatOne = dialog.findViewById<TextView>(R.id.repeat_once)
        val repeatTwo = dialog.findViewById<TextView>(R.id.repeat_two_times)
        val repeatThree = dialog.findViewById<TextView>(R.id.repeat_three_times)
        val repeatFour = dialog.findViewById<TextView>(R.id.repeat_four_times)
        val repeatInfinite = dialog.findViewById<TextView>(R.id.repeat_infinite_times)


        with(repeatOne) {
            setOnClickListener { selectRepeatOption(context, repeatOne, repeatTwo, repeatThree, repeatFour, repeatInfinite) }
        }
        with(repeatTwo) {
            setOnClickListener { selectRepeatOption(context, repeatTwo, repeatOne, repeatThree, repeatFour, repeatInfinite) }
        }
        with(repeatThree) {
            setOnClickListener { selectRepeatOption(context, repeatThree, repeatOne, repeatTwo, repeatFour, repeatInfinite) }
        }
        with(repeatFour) {
            setOnClickListener { selectRepeatOption(context, repeatFour, repeatOne, repeatTwo, repeatThree, repeatInfinite) }
        }
        with(repeatInfinite) {
            setOnClickListener { selectRepeatOption(context, repeatInfinite, repeatOne, repeatTwo, repeatThree, repeatFour) }
        }

        when (sharedPreferences.getInt("repeatTimes_${alarm.alarmType.name}", AlarmRepeatTimes.ONCE.value)) {
            AlarmRepeatTimes.ONCE.value -> selectRepeatOption(context, repeatOne, repeatTwo, repeatThree, repeatFour, repeatInfinite)
            AlarmRepeatTimes.TWO.value -> selectRepeatOption(context, repeatTwo, repeatOne, repeatThree, repeatFour, repeatInfinite)
            AlarmRepeatTimes.THREE.value -> selectRepeatOption(context, repeatThree, repeatOne, repeatTwo, repeatFour, repeatInfinite)
            AlarmRepeatTimes.FOUR.value -> selectRepeatOption(context, repeatFour, repeatOne, repeatTwo, repeatThree, repeatInfinite)
            AlarmRepeatTimes.INFINITE.value -> selectRepeatOption(context, repeatInfinite, repeatOne, repeatTwo, repeatThree, repeatFour)
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

            if(notificationSoundTextView.text.toString() != context.getString(R.string.default_tone)) {
                editor.putString("notificationSoundUri_${alarm.alarmType.name}", selectedNotificationSound!!.uri.toString())
                editor.putString("notificationSound_${alarm.alarmType.name}", selectedNotificationSound!!.title)
            } else {
                editor.remove("notificationSoundUri_${alarm.alarmType.name}")
                editor.remove("notificationSound_${alarm.alarmType.name}")
            }
            editor.putInt("repeatTimes_${alarm.alarmType.name}", getSelectedRepeatOption(repeatOne, repeatTwo, repeatThree, repeatFour, repeatInfinite))

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
        }) { _selectedNotificationSound ->
            notificationSoundTextView.text = _selectedNotificationSound.title
            selectedNotificationSound = _selectedNotificationSound
            dialog.dismiss()
        }
        notificationSoundsList.adapter = notificationSoundAdapter

        dialog.show()
    }

    private fun getNotificationSounds(context: Context): List<NotificationSound> {
        val ringtoneManager = RingtoneManager(context).apply {
            setType(RingtoneManager.TYPE_ALARM)
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
        repeatOnce: TextView,
        repeatTwo: TextView,
        repeatThree: TextView,
        repeatFour: TextView,
        repeatInfinite: TextView
    ): Int {
        return when {
            repeatOnce.backgroundTintList == ContextCompat.getColorStateList(repeatOnce.context, R.color.colorSecondary) -> 1
            repeatTwo.backgroundTintList == ContextCompat.getColorStateList(repeatTwo.context, R.color.colorSecondary) -> 2
            repeatThree.backgroundTintList == ContextCompat.getColorStateList(repeatThree.context, R.color.colorSecondary) -> 3
            repeatFour.backgroundTintList == ContextCompat.getColorStateList(repeatFour.context, R.color.colorSecondary) -> 4
            repeatInfinite.backgroundTintList == ContextCompat.getColorStateList(repeatInfinite.context, R.color.colorSecondary) -> Int.MAX_VALUE
            else -> 0
        }
    }

    private fun getSelectedBatteryPercentage(batteryPercentage: Slider): Int {
        return batteryPercentage.value.toInt()
    }
}