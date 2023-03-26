package com.mikelcalvo.batteryalarm.view

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikelcalvo.batteryalarm.databinding.ActivityMainBinding
import com.mikelcalvo.batteryalarm.model.AlarmType
import com.mikelcalvo.batteryalarm.model.BatteryAlarmSettings
import com.mikelcalvo.batteryalarm.receiver.BatteryLevelReceiver

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var batteryAlarms: MutableList<BatteryAlarmSettings>
    private lateinit var batteryAlarmAdapter: BatteryAlarmAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        batteryAlarms = mutableListOf(
            BatteryAlarmSettings(alarmType = AlarmType.BATTERY_FULL),
            BatteryAlarmSettings(
                alarmType = AlarmType.BATTERY_LOW,
                batteryPercentage = 15
            )
        )

        val layoutManager = LinearLayoutManager(this)
        batteryAlarmAdapter = BatteryAlarmAdapter(batteryAlarms)

        with(binding.rvBatteryAlarms) {
            this.layoutManager = layoutManager
            this.adapter = batteryAlarmAdapter
        }

        registerBatteryLevelReceiver()
    }


    private fun registerBatteryLevelReceiver() {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = BatteryLevelReceiver()
        applicationContext.registerReceiver(receiver, intentFilter)
    }
}