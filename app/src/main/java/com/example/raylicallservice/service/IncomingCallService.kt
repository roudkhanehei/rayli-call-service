package com.example.raylicallservice.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.app.NotificationCompat
import com.example.raylicallservice.data.AppDatabase
import com.example.raylicallservice.data.CallEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionInfo
import android.provider.ContactsContract
import android.content.ContentResolver
import android.net.Uri
import android.widget.Toast
import android.Manifest
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import com.example.raylicallservice.R
import android.view.Gravity
import android.widget.Button
import android.widget.PopupWindow
import android.view.View


class IncomingCallService : Service() {
    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null
    private val CHANNEL_ID = "IncomingCallChannel"
    private val NOTIFICATION_ID = 1
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private lateinit var database: AppDatabase
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var current_imei: String? = null
    private var current_simcart: String? = null
    private var current_sim_number: String? = null
    private var current_sim_slot: Int = -1
    
    // Call state tracking
    private var wasCallRinging = false
    private var wasCallAnswered = false
    private var callStartTime: Long = 0
    private var callEndTime: Long = 0
    private var isOutgoingCall = false
    private var currentCallSimInfo: Triple<String?, String?, Int>? = null
    private var popupWindow: PopupWindow? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupPhoneStateListener()
        startForeground()
        database = AppDatabase.getDatabase(applicationContext)
        getDeviceImei()
     
       
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Incoming Calls"
            val descriptionText = "Notifications for incoming calls"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call Service")
            .setContentText("Listening for incoming calls")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getContactName(phoneNumber: String?): String? {

       

        if (phoneNumber == null) return null
        
        var contactName: String? = null
        val contentResolver: ContentResolver = applicationContext.contentResolver
       
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                  
                    if (nameIndex != -1) {
                        contactName = cursor.getString(nameIndex)
                    }
                }
            }

         
            return contactName
        } catch (e: Exception) {
            Log.e("IncomingCallService", "Error getting contact name: ${e.message}")
        }
        
        return contactName
    }


    private fun getCurrentCallSimInfo(): Triple<String?, String?, Int> {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList

                if (activeSubscriptionInfoList != null && activeSubscriptionInfoList.isNotEmpty()) {
                    // Log all available SIM cards
                    activeSubscriptionInfoList.forEach { simInfo ->
                        Log.e("SIM_DETAILS", "SIM Card - " +
                                "Slot: ${simInfo.simSlotIndex}, " +
                                "Carrier: ${simInfo.carrierName}, " +
                                "SubId: ${simInfo.subscriptionId}, " +
                                "Display Name: ${simInfo.displayName}")
                    }

                    // Try to get the active subscription for the current call
                    var activeSim: SubscriptionInfo? = null
                    
                    // First try SIM 2 (slot index 1)
                    activeSim = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(1)
                    if (activeSim == null) {
                        // If SIM 2 is not active, try SIM 1 (slot index 0)
                        activeSim = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0)
                    }

                    // If we still don't have an active SIM, use the first available one
                    if (activeSim == null && activeSubscriptionInfoList.isNotEmpty()) {
                        activeSim = activeSubscriptionInfoList[0]
                    }

                    if (activeSim != null) {
                        Log.e("CURRENT_CALL_SIM", "Active Call SIM - " +
                                "Slot: ${activeSim.simSlotIndex}, " +
                                "Carrier: ${activeSim.carrierName}, " +
                                "SubId: ${activeSim.subscriptionId}, " +
                                "Display Name: ${activeSim.displayName}")

                        return Triple(
                            activeSim.carrierName?.toString(),
                            activeSim.iccId,
                            activeSim.simSlotIndex
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IncomingCallService", "Error getting current call SIM info: ${e.message}")
        }
        return Triple(null, null, -1)
    }

    private fun handleIncomingCall(phoneNumber: String?) {
        isOutgoingCall = false
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmssSSSSSS", Locale.getDefault())
        val _currentTime = Date()
        val timestampString = dateFormat.format(_currentTime)
        val callId = timestampString
        
        val currentTime = Date()
        
        // Get contact name if available
        val contactName = getContactName(phoneNumber)
        
        // Get current call SIM info
        currentCallSimInfo = getCurrentCallSimInfo()
        val (simCarrier, simIccId, simSlot) = currentCallSimInfo ?: Triple(null, null, -1)
        
        val callEntity = CallEntity(
            callId = callId,
            phoneNumber = phoneNumber,
            timestamp = currentTime,
            callState = "RINGING",
            callDirection = "INCOMING",
            customerName = contactName,
            simcart = simCarrier,
            sim_number = simIccId,
            sim_slot = simSlot
        )

      
        serviceScope.launch {
            if(callEntity.phoneNumber != null) {
                database.callDao().insertCall(callEntity)
            }
        }
        
       
    }

    private fun handleOutgoingCall(phoneNumber: String?) {
        isOutgoingCall = true
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmssSSSSSS", Locale.getDefault())
        val _currentTime = Date()
        val timestampString = dateFormat.format(_currentTime)
        val callId = timestampString
        val currentTime = Date()
        val formattedTime = dateFormat.format(currentTime)
        
        // Get contact name if available
        val contactName = getContactName(phoneNumber)
        
        // Get current call SIM info
        currentCallSimInfo = getCurrentCallSimInfo()
        val (simCarrier, simIccId, simSlot) = currentCallSimInfo ?: Triple(null, null, -1)
        
        val callEntity = CallEntity(
            callId = callId,
            phoneNumber = phoneNumber,
            timestamp = currentTime,
            callState = "DIALING",
            callDirection = "OUTGOING",
            customerName = contactName,
            simcart = simCarrier,
            sim_number = simIccId,
            sim_slot = simSlot
        )
        }

    private fun calculateCallDuration(): Long {
        return if (callStartTime > 0 && callEndTime > 0) {
            (callEndTime - callStartTime) / 1000 // Convert to seconds
        } else {
            0
        }
    }

    private fun handleCallStateChange(phoneNumber: String?, state: String) {
        // Get current call SIM info
        currentCallSimInfo = getCurrentCallSimInfo()
        val (simCarrier, simIccId, simSlot) = currentCallSimInfo ?: Triple(null, null, -1)
        
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmssSSSSSS", Locale.getDefault())
        val _currentTime = Date()
        val timestampString = dateFormat.format(_currentTime)

        val callId = timestampString
        val currentTime = Date()
        val formattedTime = dateFormat.format(currentTime)
        
        val duration = if (state == "ENDED") {
            calculateCallDuration()
        } else {
            0
        }
        
        // Get contact name if available
        val contactName = getContactName(phoneNumber)
        
        val callEntity = CallEntity(
            callId = callId,
            phoneNumber = phoneNumber,
            timestamp = currentTime,
            callState = state,
            duration = duration,
            imei = current_imei,
            simcart = simCarrier,
            sim_number = simIccId,
            sim_slot = simSlot,
            callDirection = if (isOutgoingCall) "OUTGOING" else "INCOMING",
            customerName = contactName
        )

        if(phoneNumber != null) {
            serviceScope.launch {
                try {
                    if(phoneNumber != "") {
                        database.callDao().insertCall(callEntity)
                        // Send broadcast to update the chart with proper flags
                        val intent = Intent("CALL_STATE_CHANGED").apply {
                            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                            setPackage(packageName)
                        }
                        sendBroadcast(intent)
                    }
                } catch (e: Exception) {
                    Log.e("IncomingCallService", "Error handling call state change: ${e.message}")
                }
            }
        }
        
        Log.d("CallState", "State: $state, SIM: $simCarrier, SIM Number: $simIccId, SIM Slot: $simSlot")
    }



    private fun showIncomingCallPopup(phoneNumber: String?, simCarrier: String?) {
        Handler(Looper.getMainLooper()).post {
            try {
                val inflater = LayoutInflater.from(applicationContext)
                val popupView = inflater.inflate(R.layout.popup_incoming_call, null)

                // Convert 10dp to pixels
                val marginInPixels = (10 * applicationContext.resources.displayMetrics.density).toInt()

                // Set up the popup window
                popupWindow = PopupWindow(
                    popupView,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    true
                )

                // Set up the views
                val titleView = popupView.findViewById<TextView>(R.id.popupTitle)
                val popupLastCallView = popupView.findViewById<TextView>(R.id.popupLastCall)
                val popupCommentView = popupView.findViewById<TextView>(R.id.popupComment)
                val dismissButton = popupView.findViewById<Button>(R.id.btnDismiss)

                // Get contact name if available
                val contactName = getContactName(phoneNumber)
                val displayName = contactName ?: phoneNumber ?: "Unknown"

                // Set the text
                titleView.text = "Incoming Call"
                //popupLastCallView.text = displayName

                // Get last call information
                if (phoneNumber != null) {
                    serviceScope.launch {
                        database.callDao().getCallsByPhoneNumber(phoneNumber).collect { calls ->
                            if (calls.isNotEmpty()) {
                                val lastCall = calls.first()
                                val lastCallTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    .format(lastCall.timestamp)
                                val lastCallState = when {
                                    lastCall.callState == "ENDED" && lastCall.callDirection == "INCOMING" -> "Answered"
                                    lastCall.callState == "MISSED" -> "Missed"
                                    lastCall.callDirection == "OUTGOING" -> "Outgoing"
                                    else -> lastCall.callState
                                }
                                
                                val LastDuration = lastCall.duration
                                val LastComment = lastCall.description                   
                                val Callduration = lastCall.duration
                                // Convert duration to hours, minutes, seconds
                                val hours = Callduration / 3600
                                val minutes = (Callduration % 3600) / 60
                                val seconds = Callduration % 60

                                // Format duration string
                                val durationString = when {
                                    hours > 0 -> String.format("%dh %dm %ds", hours, minutes, seconds)
                                    minutes > 0 -> String.format("%dm %ds", minutes, seconds)
                                    else -> String.format("%ds", seconds)
                                }

                                popupLastCallView.text = "Last Call: $lastCallTime ($lastCallState) ($durationString)"
                                popupCommentView.text = LastComment ?: "No Comment"
                            } else {
                                popupLastCallView.text = "First Call"
                                popupCommentView.text = "No Comment"
                            }
                        }
                    }
                }

                // Set up dismiss button
                dismissButton.setOnClickListener {
                    popupWindow?.dismiss()
                }

                // Set window type for showing over other apps
                popupWindow?.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)

                // Get the window manager
                val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val display = windowManager.defaultDisplay
                val size = android.graphics.Point()
                display.getSize(size)

                // Calculate the width of the popup (screen width - margins)
                val popupWidth = size.x - (2 * marginInPixels)

                // Set the width of the popup
                popupWindow?.width = popupWidth

                // Calculate vertical position (center of screen)
                val screenHeight = size.y
                val popupHeight = popupView.measuredHeight
                val verticalOffset = (screenHeight - popupHeight) / 2

                // Show the popup window in the center
                popupWindow?.showAtLocation(
                    popupView,
                    Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                    0,
                    verticalOffset
                )

                // Auto dismiss after 10 seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    popupWindow?.dismiss()
                }, 10000)

            } catch (e: Exception) {
                Log.e("IncomingCallService", "Error showing popup: ${e.message}")
                // Fallback to toast if popup fails
                Toast.makeText(
                    applicationContext,
                    "Incoming call from: ${phoneNumber ?: "Unknown"}\nSIM: $simCarrier",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupPhoneStateListener() {
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                // Get current call SIM info
                val (simCarrier, simIccId, simSlot) = getCurrentCallSimInfo()
                Log.e("CALL_STATE", "Call State: $state, " +
                        "Phone: $phoneNumber, " +
                        "Using SIM Slot: $simSlot, " +
                        "Carrier: $simCarrier")

                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        wasCallRinging = true
                        wasCallAnswered = false
                        callStartTime = 0
                        callEndTime = 0
                        
                        // Show incoming call popup
                        showIncomingCallPopup(phoneNumber, simCarrier)
                    }


                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (wasCallRinging && !wasCallAnswered) {
                            handleCallStateChange(phoneNumber, "MISSED")
                        } else {
                            callEndTime = System.currentTimeMillis()
                            handleCallStateChange(phoneNumber, "ENDED")
                        }
                        wasCallRinging = false
                        wasCallAnswered = false
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        if (!wasCallRinging) {
                            handleOutgoingCall(phoneNumber)
                        }
                        wasCallAnswered = true
                        callStartTime = System.currentTimeMillis()
                    }
                }
            }
        }
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }



    override fun onDestroy() {
        super.onDestroy()
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }

    private fun getDeviceImei() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                current_imei = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
            } else {
                @Suppress("DEPRECATION")
                current_imei = telephonyManager?.deviceId
            }
        } catch (e: Exception) {
            Log.e("IncomingCallService", "Error getting IMEI: ${e.message}")
            current_imei = null
        }
    }

    
} 