package com.example.raylicallservice

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {
    companion object {
        private const val PREFS_NAME = "ApiSettings"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_API_ENDPOINT_URL = "api_endpoint_url"
        private const val KEY_SYNC_METHOD = "sync_method"
        private const val PREF_NAME = "app_settings"
        private const val KEY_DARK_MODE = "dark_mode"
    }

    private lateinit var editApiKey: EditText
    private lateinit var editApiBaseUrl: EditText
    private lateinit var editApiEndpointUrl: EditText
    private lateinit var syncMethodSpinner: Spinner
    private lateinit var themeSwitch: SwitchMaterial

    private val syncMethods = arrayOf(
        "Deative",
        "WordPress",
        "REST API"        
    )

    // Helper function to get sync method
    fun getSyncMethod(): String {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getString(KEY_SYNC_METHOD, "WordPress") ?: "WordPress"
    }

    // Helper function to set sync method
    fun setSyncMethod(method: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putString(KEY_SYNC_METHOD, method)
            apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize views
        editApiKey = findViewById(R.id.editApiKey)
        editApiBaseUrl = findViewById(R.id.editApiBaseUrl)
        editApiEndpointUrl = findViewById(R.id.editApiEndpointUrl)
        syncMethodSpinner = findViewById(R.id.sync2ServerMethod)
        val btnSaveApiSettings = findViewById<Button>(R.id.btnSaveApiSettings)
        themeSwitch = findViewById(R.id.themeSwitch)

        // Setup spinner
        setupSyncMethodSpinner()

        // Load saved values
        loadSavedValues()

        // Set up click listeners
        btnSaveApiSettings.setOnClickListener {
            saveApiSettings()
        }

        // Initialize theme switch
        loadThemePreference()
        setupThemeSwitch()
    }

    private fun setupSyncMethodSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            syncMethods
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        syncMethodSpinner.adapter = adapter

        // Set initial value from SharedPreferences
        val savedMethod = getSyncMethod()
        val position = syncMethods.indexOf(savedMethod)
        if (position != -1) {
            syncMethodSpinner.setSelection(position)
        }
    }

    private fun loadSavedValues() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        editApiKey.setText(prefs.getString(KEY_API_KEY, ""))
        editApiBaseUrl.setText(prefs.getString(KEY_API_BASE_URL, ""))
        editApiEndpointUrl.setText(prefs.getString(KEY_API_ENDPOINT_URL, ""))
        
        // Load saved sync method
        val savedMethod = getSyncMethod()
        val position = syncMethods.indexOf(savedMethod)
        if (position != -1) {
            syncMethodSpinner.setSelection(position)
        }
    }

    private fun saveApiSettings() {
        val apiKey = editApiKey.text.toString()
        val apiBaseUrl = editApiBaseUrl.text.toString()
        val apiEndpointUrl = editApiEndpointUrl.text.toString()
        val syncMethod = syncMethodSpinner.selectedItem.toString()

        if (apiKey.isBlank() || apiBaseUrl.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putString(KEY_API_KEY, apiKey)
            putString(KEY_API_BASE_URL, apiBaseUrl)
            putString(KEY_API_ENDPOINT_URL, apiEndpointUrl)
            putString(KEY_SYNC_METHOD, syncMethod)
            apply()
        }

        Toast.makeText(this, "API settings saved successfully", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun loadThemePreference() {
        val isDarkMode = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)
        themeSwitch.isChecked = isDarkMode
        updateTheme(isDarkMode)
    }

    private fun setupThemeSwitch() {
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateTheme(isChecked)
            saveThemePreference(isChecked)
        }
    }

    private fun updateTheme(isDarkMode: Boolean) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun saveThemePreference(isDarkMode: Boolean) {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().apply {
            putBoolean(KEY_DARK_MODE, isDarkMode)
            apply()
        }
    }


} 