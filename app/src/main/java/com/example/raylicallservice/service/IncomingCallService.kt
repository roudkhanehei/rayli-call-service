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

class IncomingCallService : Service() {
    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null
    private val CHANNEL_ID = "IncomingCallChannel"
    private val NOTIFICATION_ID = 1
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private lateinit var database: AppDatabase
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupPhoneStateListener()
        startForeground()
        database = AppDatabase.getDatabase(applicationContext)
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

    private fun handleIncomingCall(phoneNumber: String?) {
        val callId = UUID.randomUUID().toString()
        val currentTime = Date()
        val formattedTime = dateFormat.format(currentTime)
        
        val callEntity = CallEntity(
            callId = callId,
            phoneNumber = phoneNumber,
            timestamp = currentTime,
            callState = "RINGING"
        )
        
        serviceScope.launch {
            database.callDao().insertCall(callEntity)
        }
        
        showNotification("Call Incoming", "Incoming call from $phoneNumber")
        Log.d("IncomingCall", "Call ID: $callId, Time: $formattedTime, Number: $phoneNumber")
    }

    private fun handleCallStateChange(phoneNumber: String?, state: String) {
        val callId = UUID.randomUUID().toString()
        val currentTime = Date()
        val formattedTime = dateFormat.format(currentTime)
        
        val callEntity = CallEntity(
            callId = callId,
            phoneNumber = phoneNumber,
            timestamp = currentTime,
            callState = state
        )
        
        serviceScope.launch {
            database.callDao().insertCall(callEntity)
        }
    }

    private fun setupPhoneStateListener() {
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        handleIncomingCall(phoneNumber)
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        showNotification("Call Ended", "The call has ended")
                        handleCallStateChange(phoneNumber, "ENDED")
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        showNotification("Call Answered", "Call is in progress")
                        handleCallStateChange(phoneNumber, "IN_PROGRESS")
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
} 