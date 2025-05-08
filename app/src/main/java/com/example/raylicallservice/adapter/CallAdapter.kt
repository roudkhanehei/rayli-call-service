package com.example.raylicallservice.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.raylicallservice.R
import com.example.raylicallservice.data.CallEntity
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.widget.EditText
import com.example.raylicallservice.data.AppDatabase
import kotlinx.coroutines.launch
import android.widget.Spinner



class CallAdapter : RecyclerView.Adapter<CallAdapter.CallViewHolder>() {
    private var calls: List<CallEntity> = emptyList()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var expandedPosition = -1

    class CallViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val phoneNumberText: TextView = view.findViewById(R.id.phoneNumberText)
        val timestampText: TextView = view.findViewById(R.id.timestampText)
        val durationText: TextView = view.findViewById(R.id.durationText)
        val callStateIcon: ImageView = view.findViewById(R.id.callStateIcon)
        val customerNameText: TextView = view.findViewById(R.id.customerNameText)
        val descriptionText: TextView = view.findViewById(R.id.descriptionText)
        val callStateText: TextView = view.findViewById(R.id.callStateText)
        val callDirectionIcon: ImageView = view.findViewById(R.id.callDirectionIcon)
        val mainContent: LinearLayout = view.findViewById(R.id.mainContent)
        val expandableSection: LinearLayout = view.findViewById(R.id.expandableSection)
        val btnCall: Button = view.findViewById(R.id.btnCall)
        val btnMessage: Button = view.findViewById(R.id.btnMessage)
        val btnDetails: Button = view.findViewById(R.id.btnDetails)
        val btnWhatsApp: Button = view.findViewById(R.id.btnWhatsApp)
        val btnTelegram: Button = view.findViewById(R.id.btnTelegram)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call, parent, false)
        return CallViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        val call = calls[position]
        holder.phoneNumberText.text = call.phoneNumber ?: "Unknown Number"
        holder.timestampText.text = dateFormat.format(call.timestamp)
        holder.durationText.text = formatDuration(call.duration)
        holder.callDirectionIcon.setImageResource(if (call.callDirection == "INCOMING") R.drawable.ic_call_incoming else R.drawable.ic_call_outgoing)

        // Limit description to 30 characters with smooth ellipsis
        holder.descriptionText.text = if (call.description?.length ?: 0 > 30) {
            "${call.description?.take(30)}..."
        } else {
            call.description ?: ""
        }

        // Set customer name with ellipsis if too long
        holder.customerNameText.text = if (call.customerName?.length ?: 0 > 20) {
            "${call.customerName?.take(20)}..."
        } else {
            call.customerName ?: ""
        }
        
        // Set text color and icon based on call state
        val (textColor, iconRes) = when (call.callState) {
            "MISSED" -> Pair(android.graphics.Color.RED, android.R.drawable.ic_menu_close_clear_cancel)
            "RINGING" -> Pair(android.graphics.Color.BLUE, android.R.drawable.ic_menu_call)
            "IN_PROGRESS" -> Pair(android.graphics.Color.GREEN, android.R.drawable.ic_menu_call)
            "ENDED" -> Pair(android.graphics.Color.GREEN, android.R.drawable.ic_menu_call)
            else -> Pair(android.graphics.Color.BLACK, android.R.drawable.ic_menu_call)
        }

        holder.callStateIcon.setImageResource(iconRes)
        holder.callStateText.text = getCallStateText(call.callState)

        // Handle expandable section
        val isExpanded = position == expandedPosition
        holder.expandableSection.visibility = if (isExpanded) View.VISIBLE else View.GONE

        // Set click listener for the main content
        holder.mainContent.setOnClickListener {
            val previousExpanded = expandedPosition
            expandedPosition = if (isExpanded) -1 else position
            notifyItemChanged(previousExpanded)
            notifyItemChanged(expandedPosition)
        }

        // Set up button click listeners
        holder.btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${call.phoneNumber}")
            }
            holder.itemView.context.startActivity(intent)
        }

        holder.btnMessage.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${call.phoneNumber}")
            }
            holder.itemView.context.startActivity(intent)
        }

        holder.btnDetails.setOnClickListener {
            showCallDetailsDialog(holder.itemView.context, call)
        }

        // Set up WhatsApp button click listener
        holder.btnWhatsApp.setOnClickListener {
            val phoneNumber = call.phoneNumber?.replace("+", "")?.replace(" ", "")
            if (phoneNumber != null) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber")
                    holder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    showWhatsAppNotInstalledDialog(holder.itemView.context)
                }
            }
        }

        // Set up Telegram button click listener
        holder.btnTelegram.setOnClickListener {
            val phoneNumber = call.phoneNumber?.replace(" ", "")
            if (phoneNumber != null) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://t.me/$phoneNumber")
                    holder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    showTelegramNotInstalledDialog(holder.itemView.context)
                }
            }
        }
    }

    private fun showCallDetailsDialog(context: android.content.Context, call: CallEntity) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_call_details, null)
        
        dialogView.findViewById<TextView>(R.id.dialogPhoneNumber).text = 
            "Phone: ${call.phoneNumber ?: "Unknown"}"
        dialogView.findViewById<TextView>(R.id.dialogTimestamp).text = 
            "Time: ${dateFormat.format(call.timestamp)}"
        dialogView.findViewById<TextView>(R.id.dialogCallState).text = 
            "State: ${getCallStateText(call.callState)}"
        dialogView.findViewById<TextView>(R.id.dialogDuration).text = 
            "Duration: ${formatDuration(call.duration)}"

        val descriptionEditText = dialogView.findViewById<EditText>(R.id.dialogDescription)
        descriptionEditText.setText(call.description ?: "")

        val customerNameEditText = dialogView.findViewById<EditText>(R.id.dialogCustomerName)
        customerNameEditText.setText(call.customerName ?: "")

        val organizationEditText = dialogView.findViewById<EditText>(R.id.dialogOrganization)
        organizationEditText.setText(call.organization ?: "")

        val dialog = AlertDialog.Builder(context, R.style.CustomDialog)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.dialogButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.dialogSaveButton).setOnClickListener {

            val newDescription = descriptionEditText.text.toString()
            val newCustomerName = customerNameEditText.text.toString()
            val newOrganization = organizationEditText.text.toString()  

            val updatedCall = call.copy(
                 customerName = newCustomerName,
                organization = newOrganization,
                description = newDescription
            )
            
            // Update the call in the database
            CoroutineScope(Dispatchers.IO).launch {
                val database = AppDatabase.getDatabase(context)
                database.callDao().insertCall(updatedCall)
            }
            
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showWhatsAppNotInstalledDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("WhatsApp Not Installed")
            .setMessage("WhatsApp is not installed on your device. Would you like to install it?")
            .setPositiveButton("Install") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("market://details?id=com.whatsapp")
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // If Play Store is not installed, open in browser
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp")
                    context.startActivity(intent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTelegramNotInstalledDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Telegram Not Installed")
            .setMessage("Telegram is not installed on your device. Would you like to install it?")
            .setPositiveButton("Install") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("market://details?id=org.telegram.messenger")
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // If Play Store is not installed, open in browser
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://play.google.com/store/apps/details?id=org.telegram.messenger")
                    context.startActivity(intent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun getItemCount() = calls.size

    fun updateCalls(newCalls: List<CallEntity>) {
        calls = newCalls
        notifyDataSetChanged()
    }

    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun getCallStateText(state: String, language: String = "en"): String {
        return when (language) {
            "fa" -> when (state) {
                "RINGING" -> "تماس ورودی"
                "MISSED" -> "تماس از دست رفته"
                "IN_PROGRESS" -> "تماس در حال انجام"
                "ENDED" -> "تماس پایان یافته"
                "DIALING" -> "تماس خروجی"
                else -> "وضعیت نامشخص"
            }
            else -> when (state) {
                "RINGING" -> "Incoming Call"
                "MISSED" -> "Missed Call"
                "IN_PROGRESS" -> "Call in Progress"
                "ENDED" -> "Responsed Call"
                "DIALING" -> "Outgoing Call"
                else -> "Unknown State"
            }
        }
    }
} 