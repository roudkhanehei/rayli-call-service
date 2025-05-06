package com.example.raylicallservice.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.raylicallservice.R
import com.example.raylicallservice.data.CallEntity
import java.text.SimpleDateFormat
import java.util.Locale

class CallAdapter : RecyclerView.Adapter<CallAdapter.CallViewHolder>() {
    private var calls: List<CallEntity> = emptyList()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    class CallViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val phoneNumberText: TextView = view.findViewById(R.id.phoneNumberText)
        val timestampText: TextView = view.findViewById(R.id.timestampText)
        val callStateText: TextView = view.findViewById(R.id.callStateText)
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
        holder.callStateText.text = call.callState
    }

    override fun getItemCount() = calls.size

    fun updateCalls(newCalls: List<CallEntity>) {
        calls = newCalls
        notifyDataSetChanged()
    }
} 