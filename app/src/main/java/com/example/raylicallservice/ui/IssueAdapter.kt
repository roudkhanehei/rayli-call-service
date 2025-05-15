package com.example.raylicallservice.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.raylicallservice.R
import com.example.raylicallservice.data.IssueEntity
import java.util.Collections

class IssueAdapter(
    private var issues: List<IssueEntity>,
    private val onEditClick: (IssueEntity) -> Unit,
    private val onDeleteClick: (IssueEntity) -> Unit,
    private val onOrderChanged: (List<IssueEntity>) -> Unit
) : RecyclerView.Adapter<IssueAdapter.IssueViewHolder>() {

    class IssueViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvIssueName)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_issue, parent, false)
        return IssueViewHolder(view)
    }

    override fun onBindViewHolder(holder: IssueViewHolder, position: Int) {
        val issue = issues[position]
        holder.tvName.text = issue.issueName
        holder.btnEdit.setOnClickListener { onEditClick(issue) }
        holder.btnDelete.setOnClickListener { onDeleteClick(issue) }
    }

    override fun getItemCount() = issues.size

    fun updateIssues(newIssues: List<IssueEntity>) {
        issues = newIssues.sortedBy { it.issueOrder }
        notifyDataSetChanged()
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val mutableList = issues.toMutableList()
        Collections.swap(mutableList, fromPosition, toPosition)
        
        // Update order for all items
        mutableList.forEachIndexed { index, issue ->
            mutableList[index] = issue.copy(issueOrder = index)
        }
        
        issues = mutableList
        notifyItemMoved(fromPosition, toPosition)
        onOrderChanged(issues)
    }
} 