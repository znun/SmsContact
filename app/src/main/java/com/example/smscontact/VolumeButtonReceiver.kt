package com.example.smscontact

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class VolumeButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            Log.d("VolumeButtonReceiver", "Volume button pressed detected")
            val serviceIntent = Intent(context, VolumeButtonService::class.java).apply {
                action = "com.example.smscontact.VOLUME_BUTTON_PRESSED"
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
