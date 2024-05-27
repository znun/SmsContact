package com.example.smscontact

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class VolumeButtonAccessibilityService : AccessibilityService() {

    private var volumeButtonPressCount = 0
    private val pressThreshold = 3
    private val intervalMillis: Long = 5000 // 5 seconds
    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No need to handle this for volume button presses
    }

    override fun onInterrupt() {
        // No need to handle this for volume button presses
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("VolumeButtonService", "Accessibility Service connected")
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        serviceInfo = info
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        Log.d("VolumeButtonService", "KeyEvent received: ${event.keyCode}")
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    volumeButtonPressCount++
                    Log.d("VolumeButtonService", "Volume button pressed: $volumeButtonPressCount")
                    if (volumeButtonPressCount == 1) {
                        startPressCountDown()
                    }
                    if (volumeButtonPressCount >= pressThreshold) {
                        Log.d("VolumeButtonService", "Press threshold reached. Starting service.")
                        val serviceIntent = Intent(this, VolumeButtonService::class.java).apply {
                            action = "com.example.smscontact.VOLUME_BUTTON_PRESSED"
                        }
                        startService(serviceIntent)
                        volumeButtonPressCount = 0
                        return true
                    }
                }
            }
        }
        return super.onKeyEvent(event)
    }

    private fun startPressCountDown() {
        handler.postDelayed({
            volumeButtonPressCount = 0
            Log.d("VolumeButtonService", "Count down finished. Resetting press count.")
        }, intervalMillis)
    }
}
