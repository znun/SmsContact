package com.example.smscontact

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class VolumeButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            Log.d("VolumeButtonReceiver", "Volume button pressed detected")
            val serviceIntent = Intent(context, VolumeButtonService::class.java).apply {
                action = "com.example.smscontact.VOLUME_BUTTON_PRESSED"
            }
            context.startService(serviceIntent)
        }
    }
}
