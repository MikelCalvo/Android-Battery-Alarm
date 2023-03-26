package com.mikelcalvo.batteryalarm.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mikelcalvo.batteryalarm.R
import com.mikelcalvo.batteryalarm.model.AlarmType
import com.mikelcalvo.batteryalarm.model.BatteryAlarmSettings

class BatteryAlarmAdapter(private val alarms: List<BatteryAlarmSettings>) :
    RecyclerView.Adapter<BatteryAlarmAdapter.ViewHolder>() {

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
        }
    }
}