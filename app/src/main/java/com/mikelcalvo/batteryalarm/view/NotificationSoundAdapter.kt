package com.mikelcalvo.batteryalarm.view

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mikelcalvo.batteryalarm.R
import com.mikelcalvo.batteryalarm.model.NotificationSound

class NotificationSoundAdapter(
    private val context: Context,
    private val notificationSounds: List<NotificationSound>,
    private val itemClickListener: (Uri) -> Unit,
    private val onItemSelected: (NotificationSound) -> Unit
) : RecyclerView.Adapter<NotificationSoundAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val soundTitle: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notificationSound = notificationSounds[position]
        holder.soundTitle.text = notificationSound.title
        holder.soundTitle.setTextColor(ContextCompat.getColor(context, R.color.textColorLight))
        holder.itemView.setOnClickListener {
            itemClickListener(notificationSound.uri)
            onItemSelected(notificationSound)
        }
    }

    override fun getItemCount() = notificationSounds.size
}