package com.example.raylicallservice

import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize views
        val switchNotifications = findViewById<Switch>(R.id.switchNotifications)
        val switchDarkMode = findViewById<Switch>(R.id.switchDarkMode)
        val btnClearCache = findViewById<Button>(R.id.btnClearCache)
        val btnAbout = findViewById<Button>(R.id.btnAbout)

        // Set up click listeners
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Implement notifications toggle
            Toast.makeText(this, "Notifications ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Implement dark mode toggle
            Toast.makeText(this, "Dark mode ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        btnClearCache.setOnClickListener {
            // TODO: Implement cache clearing
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
        }

        btnAbout.setOnClickListener {
            // TODO: Show about dialog
            Toast.makeText(this, "About RayLicallservice", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 