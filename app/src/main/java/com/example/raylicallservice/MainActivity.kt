package com.example.raylicallservice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.raylicallservice.adapter.CallAdapter
import com.example.raylicallservice.data.AppDatabase
import com.example.raylicallservice.service.IncomingCallService
import com.example.raylicallservice.SettingsActivity

import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val PERMISSIONS_REQUEST_CODE = 123
    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CallAdapter
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        setupRecyclerView()
        setupDatabase()

        if (checkPermissions()) {
            //Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            startCallService()
        } else {
            requestPermissions()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.callsRecyclerView)
        adapter = CallAdapter()
        recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 1) // 2 columns
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupDatabase() {
        database = AppDatabase.getDatabase(applicationContext)
        observeCalls()
    }

    private fun observeCalls() {
        lifecycleScope.launch {
            database.callDao().getAllCalls().collect { calls ->
                adapter.updateCalls(calls)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCallService()
            } else {
                Toast.makeText(this, "All permissions are required for the app to work properly", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startCallService() {
        val serviceIntent = Intent(this, IncomingCallService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }


    
} 