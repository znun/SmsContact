package com.example.smscontact

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.telephony.SmsManager
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        prefs = getSharedPreferences("com.example.smscontact.prefs", Context.MODE_PRIVATE)

        // Debugging step: Log the user name to see if it's set correctly
        val name = prefs.getString("user_name", null)
        Log.d("MainActivity", "User name in prefs: $name")

        if (name == null || name.isEmpty()) {
            Log.d("MainActivity", "User name not set, launching NameEntryActivity")
            val intent = Intent(this, NameEntryActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val sendLocationButton: Button = findViewById(R.id.send_location_button)
        sendLocationButton.setOnClickListener {
            Log.d("LocationSMS", "Send Location button clicked")
            requestPermissionsIfNeededForButton()
        }

        val manageContactsButton: Button = findViewById(R.id.manage_contacts_button)
        manageContactsButton.setOnClickListener {
            startActivity(Intent(this, ContactManagerActivity::class.java))
        }

        val enableServiceButton: Button = findViewById(R.id.enable_service_button)
        enableServiceButton.setOnClickListener {
            prefs.edit().putBoolean("service_enabled", true).apply()
            if (isAccessibilityServiceEnabled(this, VolumeButtonAccessibilityService::class.java)) {
                startVolumeButtonService()
                Toast.makeText(this, "Service enabled", Toast.LENGTH_SHORT).show()
            } else {
                showAccessibilityServiceDialog()
            }
        }

        val disableServiceButton: Button = findViewById(R.id.disable_service_button)
//        disableServiceButton.setOnClickListener {
//            prefs.edit().putBoolean("service_enabled", false).apply()
//            stopVolumeButtonService()
//            Toast.makeText(this, "Service disabled", Toast.LENGTH_SHORT).show()

        disableServiceButton.setOnClickListener {
            prefs.edit().putBoolean("service_enabled", false).apply()
            stopVolumeButtonService()
            Toast.makeText(this, "Service disabled", Toast.LENGTH_SHORT).show()

            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

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

    private fun requestPermissionsIfNeededForButton() {
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
            requestLocationForButton()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("LocationSMS", "Permissions granted")
                requestLocationForButton()
            } else {
                Log.d("LocationSMS", "Permissions denied")
                Toast.makeText(this, "Permissions required to send location SMS.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestLocationForButton() {
        Log.d("LocationSMS", "Requesting location for button")
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
//        val userName = prefs.getString("user_name", "Unknown User")
//        val message = "My current location is: https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}. $batteryStatus. Name: $userName"
//        Log.d("LocationSMS", "Location obtained: $message")
        val userName = prefs.getString("user_name", "Unknown User")
        val message = "$userName needs help, His Current Location is: https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}\n $batteryStatus"
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

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        val myComponentName = ComponentName(context, service).flattenToString()
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(myComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun showAccessibilityServiceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Accessibility Service")
            .setMessage("To use the volume button functionality, you need to enable the accessibility service. Would you like to enable it now?")
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("No", null)
            .show()
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
