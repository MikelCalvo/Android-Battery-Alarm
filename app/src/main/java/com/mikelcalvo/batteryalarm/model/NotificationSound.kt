package com.mikelcalvo.batteryalarm.model

import android.net.Uri

data class NotificationSound(
    val title: String,
    val uri: Uri
)