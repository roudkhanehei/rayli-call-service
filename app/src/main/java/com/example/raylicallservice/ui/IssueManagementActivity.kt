package com.example.raylicallservice.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.raylicallservice.R
import com.example.raylicallservice.data.AppDatabase
import com.example.raylicallservice.data.IssueDao
import com.example.raylicallservice.data.IssueEntity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class IssueManagementActivity : AppCompatActivity() {
    private lateinit var issueDao: IssueDao
    private lateinit var adapter: IssueAdapter
    private var allIssues: List<IssueEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_issue_management)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Issue Management"
        }

        // Initialize DAO from database
        issueDao = AppDatabase.getDatabase(applicationContext).issueDao()

        setupRecyclerView()
        setupFab()
        observeIssues()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_issue_management, menu)
        
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterIssues(newText)
                return true
            }
        })
        
        return true
    }

    private fun filterIssues(query: String?) {
        if (query.isNullOrBlank()) {
            adapter.updateIssues(allIssues)
        } else {
            val filteredList = allIssues.filter { 
                it.issueName.contains(query, ignoreCase = true) 
            }
            adapter.updateIssues(filteredList)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.issuesRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 1)
        
        adapter = IssueAdapter(
            emptyList(),
            onEditClick = { issue -> showEditDialog(issue) },
            onDeleteClick = { issue -> showDeleteConfirmation(issue) },
            onOrderChanged = { issues ->
                lifecycleScope.launch {
                    issues.forEach { issue ->
                        issueDao.updateIssue(issue)
                    }
                }
            }
        )
        
        recyclerView.adapter = adapter

        // Setup drag and drop
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                adapter.moveItem(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fabAddIssue).setOnClickListener {
            showAddDialog()
        }
    }

    private fun observeIssues() {
        lifecycleScope.launch {
            issueDao.getAllIssues().collectLatest { issues ->
                allIssues = issues
                adapter.updateIssues(issues)
            }
        }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_issue, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etIssueName)     

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add New Issue")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = etName.text.toString().trim()
 
                when {
                    name.isEmpty() -> {
                        etName.error = "Issue name is required"
                        return@setOnClickListener
                    }                   
                }

                lifecycleScope.launch {
                    val lastOrder = issueDao.getMaxOrder() ?: -1
                    val issue = IssueEntity(
                        issueID = UUID.randomUUID().toString(),
                        issueName = name,
                        issueCode = "0",
                        issueStatus = "1",
                        issueOrder = lastOrder + 1
                    )
                    issueDao.insertIssue(issue)
                    dialog.dismiss()
                }
            }
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun showEditDialog(issue: IssueEntity) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_issue, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etIssueName)

        etName.setText(issue.issueName)

        AlertDialog.Builder(this)
            .setTitle("Edit Issue")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val updatedIssue = issue.copy(
                    issueName = etName.text.toString(),
                )
                lifecycleScope.launch {
                    issueDao.updateIssue(updatedIssue)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(issue: IssueEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Issue")
            .setMessage("Are you sure you want to delete this issue?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    issueDao.deleteIssue(issue)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
} 