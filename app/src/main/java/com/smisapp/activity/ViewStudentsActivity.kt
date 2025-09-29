package com.smisapp.activity

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smisapp.R
import com.smisapp.SMISApplication
import com.smisapp.adapter.StudentAdapter
import com.smisapp.data.entity.Student
import com.smisapp.data.repository.StudentRepository
import com.smisapp.data.network.FirebaseManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog

class ViewStudentsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var searchView: SearchView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSync: Button
    private lateinit var tvSyncStatus: TextView
    private lateinit var filterSpinner: Spinner

    private lateinit var adapter: StudentAdapter
    private lateinit var studentRepository: StudentRepository
    private lateinit var firebaseManager: FirebaseManager

    private var allStudents: List<Student> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_students)

        initializeViews()
        setupRepository()
        setupRecyclerView()
        setupSearchAndFilters()
        loadStudents()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewStudents)
        tvEmpty = findViewById(R.id.tvEmptyMessage)
        searchView = findViewById(R.id.searchView)
        progressBar = findViewById(R.id.progressBar)
        btnSync = findViewById(R.id.btnSync)
        tvSyncStatus = findViewById(R.id.tvSyncStatus)
        filterSpinner = findViewById(R.id.filterSpinner)

        firebaseManager = FirebaseManager.getInstance()

        // Update sync button based on login status
        updateSyncUI()
    }

    private fun setupRepository() {
        val database = (application as SMISApplication).database
        studentRepository = StudentRepository(
            database.studentDao(),
            firebaseManager
        )
    }

    private fun setupRecyclerView() {
        adapter = StudentAdapter(emptyList()) { student ->
            showStudentDetails(student)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ViewStudentsActivity)
            adapter = this@ViewStudentsActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearchAndFilters() {
        // Setup search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterStudents(newText ?: "")
                return true
            }
        })

        // Setup filter spinner
        val filterOptions = arrayOf("All", "By Name", "By Course", "By Registration")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = spinnerAdapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val query = searchView.query.toString()
                filterStudents(query)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup sync button
        btnSync.setOnClickListener {
            syncWithCloud()
        }
    }

    private fun loadStudents() {
        showLoading(true)

        lifecycleScope.launch {
            studentRepository.getAllStudents().collect { students ->
                allStudents = students
                filterStudents(searchView.query.toString())
                showLoading(false)
                updateEmptyState()
            }
        }
    }

    private fun filterStudents(query: String) {
        val filteredStudents = if (query.isEmpty()) {
            allStudents
        } else {
            val filterType = filterSpinner.selectedItemPosition
            allStudents.filter { student ->
                when (filterType) {
                    1 -> student.name.contains(query, ignoreCase = true) // By Name
                    2 -> student.course.contains(query, ignoreCase = true) // By Course
                    3 -> student.regNumber.contains(query, ignoreCase = true) // By Reg Number
                    else -> // All
                        student.name.contains(query, ignoreCase = true) ||
                                student.regNumber.contains(query, ignoreCase = true) ||
                                student.course.contains(query, ignoreCase = true) ||
                                student.email.contains(query, ignoreCase = true)
                }
            }
        }

        adapter.updateStudents(filteredStudents)
        updateEmptyState()
    }

    private fun syncWithCloud() {
        if (!firebaseManager.isUserLoggedIn()) {
            Toast.makeText(this, "Please login to sync with cloud", Toast.LENGTH_LONG).show()
            return
        }

        showLoading(true)
        tvSyncStatus.text = "Syncing with cloud..."

        lifecycleScope.launch {
            val success = studentRepository.syncAllData()

            showLoading(false)
            if (success) {
                tvSyncStatus.text = "Last sync: Just now"
                Toast.makeText(this@ViewStudentsActivity, "Sync completed successfully!", Toast.LENGTH_SHORT).show()
                loadStudents() // Refresh data
            } else {
                tvSyncStatus.text = "Sync failed"
                Toast.makeText(this@ViewStudentsActivity, "Sync failed. Check internet connection.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showStudentDetails(student: Student) {
        val details = """
            Name: ${student.name}
            Reg Number: ${student.regNumber}
            Course: ${student.course}
            Email: ${student.email.ifEmpty { "Not provided" }}
            Phone: ${student.phone.ifEmpty { "Not provided" }}
            ${if (student.isSynced) "✓ Synced to cloud" else "⚠ Local only"}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Student Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .setNeutralButton("Delete") { _, _ ->
                deleteStudent(student)
            }
            .show()
    }

    private fun deleteStudent(student: Student) {
        AlertDialog.Builder(this)
            .setTitle("Delete Student")
            .setMessage("Are you sure you want to delete ${student.name}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    studentRepository.deleteStudent(student.id)
                    loadStudents() // Refresh list
                    Toast.makeText(this@ViewStudentsActivity, "Student deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateEmptyState() {
        if (adapter.itemCount == 0) {
            tvEmpty.text = if (searchView.query.isNotEmpty()) {
                "No students found for '${searchView.query}'"
            } else {
                "No students registered yet"
            }
            tvEmpty.visibility = android.view.View.VISIBLE
            recyclerView.visibility = android.view.View.GONE
        } else {
            tvEmpty.visibility = android.view.View.GONE
            recyclerView.visibility = android.view.View.VISIBLE
        }
    }

    private fun updateSyncUI() {
        if (firebaseManager.isUserLoggedIn()) {
            btnSync.isEnabled = true
            btnSync.alpha = 1f
            tvSyncStatus.text = "Ready to sync"
        } else {
            btnSync.isEnabled = false
            btnSync.alpha = 0.5f
            tvSyncStatus.text = "Login required for sync"
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }
}