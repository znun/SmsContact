package com.example.smscontact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
class NameEntryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_name_entry)

        val nameEditText: EditText = findViewById(R.id.name_edit_text)
        val saveNameButton: Button = findViewById(R.id.save_name_button)

        saveNameButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            if (name.isNotEmpty()) {
                saveName(name)
                // Start MainActivity and clear the back stack to prevent going back to this screen
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveName(name: String) {
        val prefs = getSharedPreferences("com.example.smscontact.prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("user_name", name).apply()
        Log.d("NameEntryActivity", "User name saved: $name")
    }
}
