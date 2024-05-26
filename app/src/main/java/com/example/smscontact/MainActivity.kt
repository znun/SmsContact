package com.example.smscontact


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
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
    private val REQUEST_PERMISSIONS = 1
    private lateinit var locationCallback: LocationCallback

    private var volumeButtonPressed = false
    private val volumeButtonHandler = Handler(Looper.getMainLooper())
    private val volumeButtonRunnable = Runnable { volumeButtonPressed = false }

    private val PREFS_NAME = "com.example.rescue_mate.prefs"
    private val PREFS_KEY_CONTACTS = "contacts"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val sendLocationButton: Button = findViewById(R.id.send_location_button)
        sendLocationButton.setOnClickListener {
            Log.d("LocationSMS", "Send Location button clicked")
            requestPermissionsIfNeeded()
        }

        val manageContactsButton: Button = findViewById(R.id.manage_contacts_button)
        manageContactsButton.setOnClickListener {
            val intent = Intent(this, ContactManagerActivity::class.java)
            startActivity(intent)
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

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun sendLocationSMS(location: Location) {
        val contacts = loadContacts()
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
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val contactsJson = prefs.getString(PREFS_KEY_CONTACTS, "[]")
        return Contact.fromJson(contactsJson)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (!volumeButtonPressed) {
                volumeButtonPressed = true
                volumeButtonHandler.postDelayed(volumeButtonRunnable, 2000)
                Log.d("LocationSMS", "Volume button pressed")
                requestPermissionsIfNeeded()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (volumeButtonPressed) {
                Log.d("LocationSMS", "Volume button released")
                volumeButtonHandler.removeCallbacks(volumeButtonRunnable)
                volumeButtonPressed = false
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }
}
