package com.mikelcalvo.batteryalarm.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikelcalvo.batteryalarm.databinding.ActivityMainBinding
import com.mikelcalvo.batteryalarm.model.AlarmType
import com.mikelcalvo.batteryalarm.model.BatteryAlarmSettings
import com.mikelcalvo.batteryalarm.view.BatteryAlarmAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var batteryAlarms: MutableList<BatteryAlarmSettings>
    private lateinit var batteryAlarmAdapter: BatteryAlarmAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        batteryAlarms = mutableListOf(
            BatteryAlarmSettings(AlarmType.BATTERY_FULL),
            BatteryAlarmSettings(AlarmType.BATTERY_LOW)
        )

        val layoutManager = LinearLayoutManager(this)
        batteryAlarmAdapter = BatteryAlarmAdapter(batteryAlarms)

        with(binding.rvBatteryAlarms) {
            this.layoutManager = layoutManager
            this.adapter = batteryAlarmAdapter
        }
    }
}