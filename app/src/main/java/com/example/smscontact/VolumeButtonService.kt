//package com.example.smscontact
//
//import android.Manifest
//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.Service
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.location.Location
//import android.os.Build
//import android.os.CountDownTimer
//import android.os.IBinder
//import android.telephony.SmsManager
//import android.util.Log
//import android.widget.Toast
//import androidx.core.app.ActivityCompat
//import androidx.core.app.NotificationCompat
//import com.google.android.gms.location.*
//
//class VolumeButtonService : Service() {
//
//    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    private lateinit var locationCallback: LocationCallback
//
//    override fun onCreate() {
//        super.onCreate()
//        Log.d("VolumeButtonService", "Service created")
//
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult) {
//                for (location in locationResult.locations) {
//                    sendLocationSMS(location)
//                }
//            }
//        }
//
//        startForegroundService()
//    }
//
//    private fun startForegroundService() {
//        val channelId = "VolumeButtonServiceChannel"
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                channelId,
//                "Volume Button Service Channel",
//                NotificationManager.IMPORTANCE_LOW
//            )
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        val notification: Notification = NotificationCompat.Builder(this, channelId)
//            .setContentTitle("Volume Button Service")
//            .setContentText("Listening for volume button presses")
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .build()
//
//        startForeground(1, notification)
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        Log.d("VolumeButtonService", "Service started with action: ${intent?.action}")
//        if (intent?.action == "com.example.smscontact.VOLUME_BUTTON_PRESSED") {
//            requestLocation()
//        }
//        return START_STICKY
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
//
//    private fun requestLocation() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val locationRequest = LocationRequest.create().apply {
//            interval = 10000 // 10 seconds
//            fastestInterval = 5000 // 5 seconds
//            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        }
//
//        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
//    }
//
//    private fun sendLocationSMS(location: Location) {
//        val contacts = loadContacts()
//        if (contacts.isEmpty()) {
//            Toast.makeText(this, "No contacts available", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val message = "SOS! My current location is: https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
//        Log.d("VolumeButtonService", "Location obtained: $message")
//        for (contact in contacts) {
//            sendSMS(contact.number, message)
//        }
//        fusedLocationClient.removeLocationUpdates(locationCallback) // Stop location updates after getting the location
//    }
//
//    private fun sendSMS(phoneNumber: String, message: String) {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        try {
//            val smsManager: SmsManager = SmsManager.getDefault()
//            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
//            Toast.makeText(this, "SOS SMS Sent to $phoneNumber!", Toast.LENGTH_SHORT).show()
//            Log.d("VolumeButtonService", "SOS SMS sent to $phoneNumber")
//        } catch (e: Exception) {
//            Log.e("VolumeButtonService", "Error sending SMS: ${e.message}")
//            Toast.makeText(this, "Failed to send SOS SMS. Please try again.", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun loadContacts(): MutableList<Contact> {
//        val prefs = getSharedPreferences("com.example.smscontact.prefs", Context.MODE_PRIVATE)
//        val contactsJson = prefs.getString("contacts", "[]")
//        Log.d("VolumeButtonService", "Loaded contacts: $contactsJson")
//        return Contact.fromJson(contactsJson)
//    }
//}

//-------------------

package com.example.smscontact

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class VolumeButtonService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        Log.d("VolumeButtonService", "Service created")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    sendLocationSMS(location)
                }
            }
        }

        startForegroundService()
    }

    private fun startForegroundService() {
        val channelId = "VolumeButtonServiceChannel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Volume Button Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Volume Button Service")
            .setContentText("Listening for volume button presses")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("VolumeButtonService", "Service started with action: ${intent?.action}")
        if (intent?.action == "com.example.smscontact.VOLUME_BUTTON_PRESSED") {
            requestLocation()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun sendLocationSMS(location: Location) {
        val contacts = loadContacts()
        Log.d("MainActivity", "Loaded contacts: $contacts")
        if (contacts.isEmpty()) {
            Toast.makeText(this, "No contacts available", Toast.LENGTH_SHORT).show()
            return
        }

        val batteryStatus = getBatteryStatus()
        val message = "My current location is: https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}. $batteryStatus"
        Log.d("LocationSMS", "Location obtained: $message")
        for (contact in contacts) {
            sendSMS(contact.number, message)
        }
        fusedLocationClient.removeLocationUpdates(locationCallback) // Stop location updates after getting the location
    }


    private fun sendSMS(phoneNumber: String, message: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val smsManager: SmsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "SOS SMS Sent to $phoneNumber!", Toast.LENGTH_SHORT).show()
            Log.d("VolumeButtonService", "SOS SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e("VolumeButtonService", "Error sending SMS: ${e.message}")
            Toast.makeText(this, "Failed to send SOS SMS. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadContacts(): MutableList<Contact> {
        val prefs = getSharedPreferences("com.example.smscontact.prefs", Context.MODE_PRIVATE)
        val contactsJson = prefs.getString("contacts", "[]")
        Log.d("VolumeButtonService", "Loaded contacts: $contactsJson")
        return Contact.fromJson(contactsJson)
    }
    private fun getBatteryStatus(): String {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            this.registerReceiver(null, ifilter)
        }

        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level / scale.toFloat() * 100
        }

        return "Battery Level: ${batteryPct?.toInt()}%"
    }
}