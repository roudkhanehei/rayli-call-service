package com.example.raylicallservice

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {
    companion object {
        private const val PREFS_NAME = "ApiSettings"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_BASE_URL = "api_base_url"
    }

    private lateinit var editApiKey: EditText
    private lateinit var editApiBaseUrl: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize views
        editApiKey = findViewById(R.id.editApiKey)
        editApiBaseUrl = findViewById(R.id.editApiBaseUrl)
        val btnSaveApiSettings = findViewById<Button>(R.id.btnSaveApiSettings)
        val switchNotifications = findViewById<Switch>(R.id.switchNotifications)
      
    
        // Load saved values
        loadSavedValues()

        // Set up click listeners
        btnSaveApiSettings.setOnClickListener {
            saveApiSettings()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Implement notifications toggle
            Toast.makeText(this, "Notifications ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }


     
    }

    private fun loadSavedValues() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        editApiKey.setText(prefs.getString(KEY_API_KEY, ""))
        editApiBaseUrl.setText(prefs.getString(KEY_API_BASE_URL, ""))
    }

    private fun saveApiSettings() {
        val apiKey = editApiKey.text.toString()
        val apiBaseUrl = editApiBaseUrl.text.toString()

        if (apiKey.isBlank() || apiBaseUrl.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putString(KEY_API_KEY, apiKey)
            putString(KEY_API_BASE_URL, apiBaseUrl)
            apply()
        }

        Toast.makeText(this, "API settings saved successfully", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 