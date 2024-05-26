package com.example.smscontact

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.telephony.SmsManager
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val REQUEST_PERMISSIONS = 1
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val sendLocationButton: Button = findViewById(R.id.send_location_button)
        sendLocationButton.setOnClickListener {
            Log.d("LocationSMS", "Send Location button clicked")
            requestPermissionsIfNeeded()
        }

        val manageContactsButton: Button = findViewById(R.id.manage_contacts_button)
        manageContactsButton.setOnClickListener {
            startActivity(Intent(this, ContactManagerActivity::class.java))
        }

        val enableServiceButton: Button = findViewById(R.id.enable_service_button)
        enableServiceButton.setOnClickListener {
            prefs.edit().putBoolean("service_enabled", true).apply()
            startVolumeButtonService()
            Toast.makeText(this, "Service enabled", Toast.LENGTH_SHORT).show()
        }

        val disableServiceButton: Button = findViewById(R.id.disable_service_button)
        disableServiceButton.setOnClickListener {
            prefs.edit().putBoolean("service_enabled", false).apply()
            stopVolumeButtonService()
            Toast.makeText(this, "Service disabled", Toast.LENGTH_SHORT).show()
        }

        // Start the service if it is enabled in preferences
        if (prefs.getBoolean("service_enabled", false)) {
            startVolumeButtonService()
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    sendLocationSMS(location)
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissionsNeeded = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS)
        }

        if (permissionsNeeded.isNotEmpty()) {
            Log.d("LocationSMS", "Requesting permissions: $permissionsNeeded")
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQUEST_PERMISSIONS)
        } else {
            requestLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("LocationSMS", "Permissions granted")
                requestLocation()
            } else {
                Log.d("LocationSMS", "Permissions denied")
                Toast.makeText(this, "Permissions required to send location SMS.", Toast.LENGTH_SHORT).show()
            }
        }
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

        val message = "My current location is: https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
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
            Toast.makeText(this, "SMS Sent to $phoneNumber!", Toast.LENGTH_SHORT).show()
            Log.d("LocationSMS", "SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e("LocationSMS", "Error sending SMS: ${e.message}")
            Toast.makeText(this, "Failed to send SMS. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadContacts(): MutableList<Contact> {
        val prefs = getSharedPreferences("com.example.smscontact.prefs", Context.MODE_PRIVATE)
        val contactsJson = prefs.getString("contacts", "[]")
        Log.d("MainActivity", "Loaded contacts: $contactsJson")
        return Contact.fromJson(contactsJson)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val intent = Intent(this, VolumeButtonService::class.java).apply {
                action = "com.example.smscontact.VOLUME_BUTTON_PRESSED"
            }
            startService(intent)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun startVolumeButtonService() {
        val intent = Intent(this, VolumeButtonService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopVolumeButtonService() {
        val intent = Intent(this, VolumeButtonService::class.java)
        stopService(intent)
    }
}
