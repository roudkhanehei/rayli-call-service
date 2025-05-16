package com.example.raylicallservice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.raylicallservice.adapter.CallAdapter
import com.example.raylicallservice.data.AppDatabase
import com.example.raylicallservice.databinding.ActivityMainBinding
import com.example.raylicallservice.service.IncomingCallService
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.*
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.Legend.LegendVerticalAlignment
import android.widget.Toast
import kotlinx.coroutines.launch
import com.github.mikephil.charting.formatter.ValueFormatter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import android.view.View
import android.widget.TextView
import android.net.Uri
import android.provider.Settings
import com.example.raylicallservice.api.RetrofitClient
import com.example.raylicallservice.api.CallData
import com.example.raylicallservice.api.ApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.appcompat.app.AlertDialog
import android.provider.ContactsContract
import android.content.SharedPreferences
import android.widget.ImageView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import com.example.raylicallservice.ui.IssueManagementActivity
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {
    private val PERMISSIONS_REQUEST_CODE = 123
    private val OVERLAY_PERMISSION_REQ_CODE = 124
    private val KEY_SYNC_METHOD = "sync_method"
    private val KEY_DARK_MODE = "dark_mode"
    private val PREF_NAME = "app_settings"

    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_PHONE_NUMBERS
        )
    } else {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_NUMBERS
        )
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CallAdapter
    private lateinit var database: AppDatabase
    private lateinit var sharedPreferences: SharedPreferences
    private var searchView: SearchView? = null

    private val callStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "CALL_STATE_CHANGED") {
                try {
                    setupWeeklyCallsChart()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating chart: ${e.message}")
                }
            }
        }
    }

    private fun initializeTheme() {
        val isDarkMode = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("ApiSettings", Context.MODE_PRIVATE)

        // Register broadcast receiver with proper flags
        val filter = IntentFilter("CALL_STATE_CHANGED").apply {
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }
        registerReceiver(callStateReceiver, filter, Context.RECEIVER_EXPORTED)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            val syncMethod = sharedPreferences.getString(KEY_SYNC_METHOD, "Deative")
            if (syncMethod == "WordPress") {
                makeApiCall()
            } else if (syncMethod == "REST API") {
                makeCustomerApiCall()
            } else if (syncMethod == "Deative") {
                Toast.makeText(this, "Sync method is Deative , if you want to sync data to server go to Settings", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Update toolbar subtitle
        updateToolbarSubtitle()
        
        setupRecyclerView()
        setupDatabase()
        setupWeeklyCallsChart()

        if (checkPermissions()) {
            startCallService()
        } else {
            requestPermissions()
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        }

        // Initialize theme
        initializeTheme()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(callStateReceiver)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error unregistering receiver: ${e.message}")
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        // Setup search functionality
        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView
        searchView?.apply {
            queryHint = "Search by name, phone, or comment..."
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    filterCalls(query)
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filterCalls(newText)
                    return true
                }
            })

            // Handle search close
            setOnCloseListener {
                adapter.clearFilter()
                true
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Handle settings click
                showSettingsDialog()
                true
            }
            R.id.filter_all -> {
                adapter.filterByCallState(null,null)
                true
            }
            R.id.filter_missed -> {
                adapter.filterByCallState("MISSED","INCOMING")
                true
            }
            R.id.filter_answered -> {
                adapter.filterByCallState("ENDED","INCOMING")
                true
            }
            R.id.filter_outgoing -> {
                adapter.filterByCallState("ENDED","OUTGOING")
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        adapter = CallAdapter()
        binding.callsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 1)
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
                // Show/hide empty state container
                binding.emptyStateContainer.visibility = if (calls.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSIONS_REQUEST_CODE)
            return false
        }
        return true
    }

    private fun requestPermissions() {
        val permissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }

            if (deniedPermissions.isEmpty()) {
                // All permissions granted
                startCallService()
            } else {
                // Some permissions were denied
                val shouldShowRationale = deniedPermissions.any { permission ->
                    shouldShowRequestPermissionRationale(permission)
                }

                if (shouldShowRationale) {
                    // Show explanation why permissions are needed
                    showPermissionExplanationDialog(deniedPermissions)
                } else {
                    // Permissions permanently denied, show settings dialog
                    showSettingsDialog()
                }
            }
        }
    }

    private fun showPermissionExplanationDialog(deniedPermissions: List<String>) {
        val message = when {
            deniedPermissions.contains(Manifest.permission.READ_CONTACTS) -> 
                "Contacts permission is required to access contact information for incoming calls."
            deniedPermissions.contains(Manifest.permission.READ_CALL_LOG) ->
                "Call log permission is required to track call history."
            deniedPermissions.contains(Manifest.permission.READ_PHONE_STATE) ->
                "Phone state permission is required to detect incoming and outgoing calls."
            else -> "All permissions are required for the app to work properly."
        }

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant Permissions") { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showSettingsDialog() {
        val items = arrayOf(
            "Preferences",
            "Issue Management",
            "Sync Now",
            "Clear All Data",
            "About"
        )
        
        val icons = arrayOf(
            R.drawable.ic_settings,
            R.drawable.ic_settings,
            R.drawable.ic_sync,
            R.drawable.ic_delete,
            R.drawable.ic_info
        )

        val dialog = AlertDialog.Builder(this)
            .setTitle("Settings")
            .setAdapter(object : ArrayAdapter<String>(
                this,
                R.layout.dialog_item_with_icon,
                items
            ) {
                override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                    val view = convertView ?: layoutInflater.inflate(R.layout.dialog_item_with_icon, parent, false)
                    
                    val iconView = view.findViewById<ImageView>(R.id.dialog_item_icon)
                    val textView = view.findViewById<TextView>(R.id.dialog_item_text)
                    
                    iconView.setImageResource(icons[position])
                    textView.text = items[position]
                    
                    return view
                }
            }) { _, which ->
                when (which) {
                    0 -> showPreferencesDialog()
                    1 -> showIssueManagementDialog()
                    2 -> makeApiCall() // Sync Now
                    3 -> showClearDataConfirmation() // Clear All Data
                    4 -> showAboutDialog() // About
                }
            }
            .create()
        
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun showPreferencesDialog() {
       Intent(this, SettingsActivity::class.java).apply {
        startActivity(this)
       }
    }

    private fun showIssueManagementDialog() {
        Intent(this, IssueManagementActivity::class.java).apply {
            startActivity(this)
        }
    }

    private fun showClearDataConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Data")
            .setMessage("Are you sure you want to clear all call records? This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                lifecycleScope.launch {
                    database.callDao().deleteAllCalls()
                    Toast.makeText(this@MainActivity, "All data cleared", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About")
            .setMessage("Rayli Call Manager\nVersion 1.0.5\n\nA call tracking and management application.\n\nDeveloped by\nPejman Roudkhanehei Shalmani")
            .setPositiveButton("OK", null)
            .create()
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission is required for call notifications", Toast.LENGTH_LONG).show()
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


    //Create a chart to show the number of calls per day for the last 7 days

    private fun setupWeeklyCallsChart() {        // Get data for the last 7 days


        lifecycleScope.launch {

            val totalResponsedCallsTextTV = findViewById<TextView>(R.id.totalResponsedCallsText)
            val missedCallsTextTV = findViewById<TextView>(R.id.missedCallsText)
            val answeredCallsTextVT = findViewById<TextView>(R.id.answeredCallsText)
            val statisticsTextTV = findViewById<TextView>(R.id.statisticsText)

            val calendar = Calendar.getInstance()
            val endDate = calendar.time 
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val startDate = calendar.time


            val totalCallsForDayEnded = database.callDao().getCallsBetweenDates(startDate, endDate)
            .filter { it.callState == "ENDED" && it.callDirection == "INCOMING" }

            val totalCallsForDayMissed = database.callDao().getCallsBetweenDates(startDate, endDate)
            .filter { it.callState == "MISSED" && it.callDirection == "INCOMING" }

            val totalCallsForDayIncoming = database.callDao().getCallsBetweenDates(startDate, endDate)
            .filter { it.callDirection == "OUTGOING" && it.callState == "ENDED" }

            // Calculate total duration for all calls
            val totalDuration = database.callDao().getCallsBetweenDates(startDate, endDate)
                .sumOf { it.duration }

            // Convert duration to hours, minutes, seconds
            val hours = totalDuration / 3600
            val minutes = (totalDuration % 3600) / 60
            val seconds = totalDuration % 60

            // Format duration string
            val durationString = when {
                hours > 0 -> String.format("%dh %dm %ds", hours, minutes, seconds)
                minutes > 0 -> String.format("%dm %ds", minutes, seconds)
                else -> String.format("%ds", seconds)
            }

            totalResponsedCallsTextTV.text = totalCallsForDayEnded.size.toString()
            missedCallsTextTV.text = totalCallsForDayMissed.size.toString()
            answeredCallsTextVT.text = totalCallsForDayIncoming.size.toString()
            statisticsTextTV.text = "Statistics ($durationString)"

        }


        
        lifecycleScope.launch {
            val calendar = Calendar.getInstance()


            val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
            val entries = ArrayList<BarEntry>()
            val entries2 = ArrayList<BarEntry>()
            val entries3 = ArrayList<BarEntry>()
            val labels = ArrayList<String>()
    
            
            // Get calls for the last 7 days
            for (i in 7 downTo 1) {
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val startOfDay = calendar.time

                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val endOfDay = calendar.time
                
           
                calendar.add(Calendar.DAY_OF_YEAR, -1) // Reset to start of day

                val callsForDayEnded = database.callDao().getCallsForDay(endOfDay)
                .filter { it.callState == "ENDED" && it.callDirection == "INCOMING" }
                val callsForDayMissed = database.callDao().getCallsForDay(endOfDay)
                .filter { it.callState == "MISSED" && it.callDirection == "INCOMING" }
                val callsForDayIncoming = database.callDao().getCallsForDay(endOfDay)
                .filter { it.callDirection == "OUTGOING" && it.callState == "ENDED" }

                val dayLabel = dateFormat.format(endOfDay)
                labels.add(dayLabel)
                
                entries.add(BarEntry((7 - i).toFloat(), callsForDayEnded.size.toFloat()))
                entries2.add(BarEntry((7 - i).toFloat(), callsForDayMissed.size.toFloat()))
                entries3.add(BarEntry((7 - i).toFloat(), callsForDayIncoming.size.toFloat()))
                
                calendar.add(Calendar.DAY_OF_YEAR, i) // Reset calendar
            }

            val dataSet = BarDataSet(entries, "Responsed")
            val dataSet2 = BarDataSet(entries2, "Missed")
            val dataSet3 = BarDataSet(entries3, "Outgoing")
            
            // Set specific colors for each dataset
            dataSet.color = Color.parseColor("#4CAF50")  
            dataSet2.color = Color.parseColor("#FF9800")  
            dataSet3.color = Color.parseColor("#3F51B5")  

            // Customize value text for all datasets
            listOf(dataSet, dataSet2, dataSet3).forEach { set ->
                set.valueTextSize = 10f
                set.valueTextColor = Color.BLACK
                set.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value.toInt() != 0) value.toInt().toString() else ""
                    }
                }
            }
            
            // Add color information to legend
            binding.weeklyCallsChart.legend.apply {
                isEnabled = true
                textSize = 12f
                formSize = 12f
                formToTextSpace = 5f
                xEntrySpace = 10f
                yEntrySpace = 20f    
              
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
            
            // Configure chart appearance
            binding.weeklyCallsChart.apply {
                description.isEnabled = false
                description.text = "Last 7 Days Call Statistics"
                setDrawGridBackground(false)
                setDrawBarShadow(false)
                setScaleEnabled(true)
                setPinchZoom(false)
                setExtraOffsets(0f, 0f, 0f, 20f)  // Add Y padding (left, top, right, bottom)
                
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    textSize = 12f
                    textColor = Color.BLACK

                }
                
                axisLeft.apply {
                    setDrawGridLines(true)
                    axisMinimum = 0f
                    textSize = 12f
                    textColor = Color.BLACK
                }
                
                axisRight.isEnabled = false
            }
            
            // Create BarData with all datasets
            val data = BarData(dataSet, dataSet2, dataSet3)
            data.barWidth = 0.25f // Make bars thinner to fit multiple datasets
            
            // Set the data and refresh
            binding.weeklyCallsChart.data = data
            binding.weeklyCallsChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            binding.weeklyCallsChart.groupBars(0f, 0.1f, 0.02f) // Group the bars
            binding.weeklyCallsChart.animateY(1000)
            binding.weeklyCallsChart.invalidate()

        }
    }

    private fun getContactName(phoneNumber: String): String? {
        if (!hasContactPermission()) {
            return null
        }

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor = contentResolver.query(uri, projection, null, null, null)
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        it.getString(nameIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting contact name: ${e.message}")
            null
        }
    }

    private fun hasContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun saveLastSyncDate() {
        val currentTime = System.currentTimeMillis()
        sharedPreferences.edit().apply {
            putLong("last_sync_time", currentTime)
            apply()
        }
    }

    private fun getLastSyncDate(): String {
        val lastSyncTime = sharedPreferences.getLong("last_sync_time", 0L)
        return if (lastSyncTime > 0) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            dateFormat.format(Date(lastSyncTime))
        } else {
            "Never"
        }
    }

    private fun updateToolbarSubtitle() {
        binding.toolbar.subtitle = "Last Sync: ${getLastSyncDate()}"
        binding.toolbar.setSubtitleTextColor(Color.WHITE)
        binding.toolbar.setSubtitleTextAppearance(this, R.style.ToolbarSubtitleStyle)
    }

    private fun makeApiCall() {
        // Create and show progress dialog
        val progressDialog = AlertDialog.Builder(this)
            .setView(R.layout.progress_dialog)
            .setCancelable(false)
            .create()
        
        progressDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        progressDialog.show()

        // Get reference to progress text
        val progressTextView = progressDialog.findViewById<TextView>(R.id.syncProgress_text)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get all unsynced calls
                val unsyncedCalls = database.callDao().getUnsyncedCalls()
                var syncedCount = 0
                var lastResponse: retrofit2.Response<ApiResponse>? = null
                
                if (unsyncedCalls.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "No unsynced calls found", Toast.LENGTH_SHORT).show()
                    }
                    progressDialog.dismiss()
                }else{
                // Update their sync status
                unsyncedCalls.forEach { call ->            
                    withContext(Dispatchers.Main) {
                        progressTextView?.text = "Syncing call ${syncedCount + 1} of ${unsyncedCalls.size}..."
                    }
            
                   
          

                   
                    // Convert CallEntity to CallData
                    val callData = CallData(
                        call_id = call.callId.toString(),
                        caller_number = call.phoneNumber ?: "",
                        call_duration = call.duration ?: 0,
                        call_status = call.callState,
                        call_date = convertTimestampToDate(call.timestamp).toString(),
                        customer_name = call.customerName ?: "",
                        products_id = call.productsId ?: "0",
                        description = call.description ?: "", 
                        organization = call.organization ?: "",
                        customer_id = call.customerId?.toLong() ?: 0,
                        simcart = call.simcart ?: "",
                        sim_number = call.sim_number ?: "",
                        sim_slot = call.sim_slot?.toString() ?: "",
                        issue = "",
                        additional_data = "",
                        imei = call.imei ?: "",
                        call_direction = call.callDirection ?: "",
                        is_synced = true
                    )

                    //now send the data to the server

                    lastResponse = RetrofitClient.getInstance(this@MainActivity)
                    .apiService.postCallData( "wp-json/rayli-call-manager/v1/receive-call-data", callData)

                    if (lastResponse?.isSuccessful == true) {
                        // Update call sync status after successful API call
                        database.callDao().updateCallSyncStatus(call.callId, true)
                    }
                 
                    syncedCount++
                }
               
                withContext(Dispatchers.Main) {
                    // Dismiss progress dialog
                    progressDialog.dismiss()
                    
                    if (lastResponse?.isSuccessful == true) {
                        // Save sync time after successful API call
                        saveLastSyncDate()
                        // Update toolbar subtitle
                        updateToolbarSubtitle()
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "Call data sent successfully: ${lastResponse?.body()?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to send call data: ${lastResponse?.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Dismiss progress dialog
                    progressDialog.dismiss()
                    
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun makeCustomerApiCall() {
        // Get API endpoint from SharedPreferences
        val apiEndpoint = sharedPreferences.getString("api_endpoint_url", "") ;
        
       
        // Create and show progress dialog
        val progressDialog = AlertDialog.Builder(this)
            .setView(R.layout.progress_dialog)
            .setCancelable(false)
            .create()
        
        progressDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        progressDialog.show()

        // Get reference to progress text
        val progressTextView = progressDialog.findViewById<TextView>(R.id.syncProgress_text)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get all unsynced calls
                val unsyncedCalls = database.callDao().getUnsyncedCalls()
                var syncedCount = 0
                var lastResponse: retrofit2.Response<ApiResponse>? = null
                
                if (unsyncedCalls.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "No unsynced calls found", Toast.LENGTH_SHORT).show()
                    }
                    progressDialog.dismiss()
                } else {
                    // Update their sync status
                    unsyncedCalls.forEach { call ->            
                        withContext(Dispatchers.Main) {
                            progressTextView?.text = "Syncing call ${syncedCount + 1} of ${unsyncedCalls.size}..."
                        }
                
                        // Convert CallEntity to CallData
                        val callData = CallData(
                            call_id = call.callId.toString(),
                            caller_number = call.phoneNumber ?: "",
                            call_duration = call.duration ?: 0,
                            call_status = call.callState,
                            call_date = convertTimestampToDate(call.timestamp).toString(),
                            customer_name = call.customerName ?: "",
                            products_id = call.productsId ?: "0",
                            description = call.description ?: "", 
                            organization = call.organization ?: "",
                            customer_id = call.customerId?.toLong() ?: 0,
                            simcart = call.simcart ?: "",
                            sim_number = call.sim_number ?: "",
                            sim_slot = call.sim_slot?.toString() ?: "",
                            issue =  "",
                            additional_data = "",
                            imei = call.imei ?: "",
                            call_direction = call.callDirection ?: "",
                            is_synced = true
                        )
                        //now send the data to the server using the dynamic endpoint
                        lastResponse = RetrofitClient.getInstance(this@MainActivity).apiService.postCallData(apiEndpoint.toString(), callData)

                        if (lastResponse?.isSuccessful == true) {
                            // Update call sync status after successful API call
                            database.callDao().updateCallSyncStatus(call.callId, true)
                        }
                     
                        syncedCount++
                    }
                
                    withContext(Dispatchers.Main) {
                        // Dismiss progress dialog
                        progressDialog.dismiss()
                        
                        if (lastResponse?.isSuccessful == true) {
                            // Save sync time after successful API call
                            saveLastSyncDate()
                            // Update toolbar subtitle
                            updateToolbarSubtitle()
                            
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Call data sent successfully: ${lastResponse?.body()?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Failed to send call data: ${lastResponse?.code()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Dismiss progress dialog
                    progressDialog.dismiss()
                    
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        // Check permissions again when returning to the app
        if (checkPermissions()) {
            startCallService()
        }
    }

    private fun convertTimestampToDate(timestamp: Date): String {
        try {
            // English format
            val englishFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            return englishFormat.format(timestamp)
        } catch (e: Exception) {
            return "Invalid Date"
        }
    }

    private fun filterCalls(query: String?) {
        if (query.isNullOrBlank()) {
            adapter.clearFilter()
            return
        }

        val searchQuery = query.lowercase()
        adapter.filterCalls { call ->
            val customerName = call.customerName?.lowercase() ?: ""
            val phoneNumber = call.phoneNumber?.lowercase() ?: ""
            val description = call.description?.lowercase() ?: ""

            customerName.contains(searchQuery) ||
            phoneNumber.contains(searchQuery) ||
            description.contains(searchQuery)
        }
    }
}