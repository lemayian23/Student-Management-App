package com.smisapp.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.smisapp.R
import com.smisapp.SMISApplication
import com.smisapp.adapter.ShimmerStudentAdapter
import com.smisapp.adapter.StudentAdapter
import com.smisapp.animation.StudentItemAnimator
import com.smisapp.animation.StudentItemDecoration
import com.smisapp.data.entity.Student
import com.smisapp.data.repository.Resource
import com.smisapp.data.repository.StudentRepository
import com.smisapp.data.network.FirebaseManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ViewStudentsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewShimmer: RecyclerView
    private lateinit var emptyStateView: LinearLayout
    private lateinit var ivEmptyIllustration: ImageView
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var btnAddFirstStudent: Button
    private lateinit var searchView: SearchView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSync: Button
    private lateinit var tvSyncStatus: TextView
    private lateinit var filterSpinner: Spinner
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var adapter: StudentAdapter
    private lateinit var shimmerAdapter: ShimmerStudentAdapter
    private lateinit var studentRepository: StudentRepository
    private lateinit var firebaseManager: FirebaseManager

    private var allStudents: List<Student> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_students)

        initializeViews()
        setupRepository() // This will now work
        setupRecyclerView()
        setupShimmerRecyclerView()
        setupSearchAndFilters()
        setupSwipeRefresh()
        setupEmptyStateActions()
        loadStudents()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewStudents)
        recyclerViewShimmer = findViewById(R.id.recyclerViewShimmer)
        emptyStateView = findViewById(R.id.emptyStateView)
        ivEmptyIllustration = findViewById(R.id.ivEmptyIllustration)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle)
        btnAddFirstStudent = findViewById(R.id.btnAddFirstStudent)
        searchView = findViewById(R.id.searchView)
        progressBar = findViewById(R.id.progressBar)
        btnSync = findViewById(R.id.btnSync)
        tvSyncStatus = findViewById(R.id.tvSyncStatus)
        filterSpinner = findViewById(R.id.filterSpinner)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        firebaseManager = FirebaseManager.getInstance()
        updateSyncUI()
    }

    // ADD THIS MISSING METHOD
    private fun setupRepository() {
        val database = (application as SMISApplication).database
        studentRepository = StudentRepository(
            database.studentDao(),
            firebaseManager
        )
    }

    private fun setupRecyclerView() {
        adapter = StudentAdapter(
            students = emptyList(),
            onItemClick = { student ->
                showStudentDetails(student)
            },
            onMoreActionsClick = { student, view ->
                showMoreActionsMenu(student, view)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ViewStudentsActivity)
            adapter = this@ViewStudentsActivity.adapter
            setHasFixedSize(true)
            itemAnimator = StudentItemAnimator()
            addItemDecoration(StudentItemDecoration(
                resources.getDimensionPixelSize(R.dimen.student_item_spacing)
            ))
        }
    }

    // Add this new method for the actions menu
    private fun showMoreActionsMenu(student: Student, anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        // For now, let's create a simple menu programmatically to avoid XML issues
        popup.menu.add("Edit").setIcon(android.R.drawable.ic_menu_edit)
        popup.menu.add("Share").setIcon(android.R.drawable.ic_menu_share)
        popup.menu.add("QR Code").setIcon(android.R.drawable.ic_menu_camera)

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Edit" -> {
                    // LAUNCH EDIT ACTIVITY - FIXED INTENT
                    try {
                        val intent = Intent(this@ViewStudentsActivity, EditStudentActivity::class.java)
                        intent.putExtra(EditStudentActivity.EXTRA_STUDENT_ID, student.id)
                        startActivity(intent)
                    } catch (e: Exception) {
                        showToast("Edit feature not available yet")
                        e.printStackTrace()
                    }
                    true
                }
                "Share" -> {
                    showToast("Share ${student.name} - Coming soon! 📤")
                    true
                }
                "QR Code" -> {
                    showToast("QR Code for ${student.name} - Coming soon! 📱")
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    // REMOVE THIS DUPLICATE METHOD - You had two setupRecyclerView() methods!
    /*
    private fun setupRecyclerView() {
        adapter = StudentAdapter(emptyList()) { student ->
            showStudentDetails(student)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ViewStudentsActivity)
            adapter = this@ViewStudentsActivity.adapter
            setHasFixedSize(true)
            itemAnimator = StudentItemAnimator()
            addItemDecoration(StudentItemDecoration(
                resources.getDimensionPixelSize(R.dimen.student_item_spacing)
            ))
        }
    }
    */

    private fun setupShimmerRecyclerView() {
        shimmerAdapter = ShimmerStudentAdapter()

        recyclerViewShimmer.apply {
            layoutManager = LinearLayoutManager(this@ViewStudentsActivity)
            adapter = shimmerAdapter
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSearchAndFilters() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterStudents(newText ?: "")
                return true
            }
        })

        val filterOptions = arrayOf("All", "By Name", "By Course", "By Registration")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = spinnerAdapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                filterStudents(searchView.query.toString())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSync.setOnClickListener {
            syncWithCloud()
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            loadStudents()
        }
    }

    private fun setupEmptyStateActions() {
        btnAddFirstStudent.setOnClickListener {
            showToast("Add Student feature coming soon! 🎉")
        }
    }

    private fun loadStudents() {
        showShimmerLoading(true)

        lifecycleScope.launch {
            studentRepository.getAllStudents().collect { students ->
                allStudents = students
                filterStudents(searchView.query.toString())
                showShimmerLoading(false)
                swipeRefreshLayout.isRefreshing = false
                updateEmptyState()
            }
        }
    }

    private fun showShimmerLoading(show: Boolean) {
        if (show) {
            // Show shimmer, hide actual content and empty state
            recyclerViewShimmer.visibility = android.view.View.VISIBLE
            recyclerView.visibility = android.view.View.GONE
            emptyStateView.visibility = android.view.View.GONE
            progressBar.visibility = android.view.View.GONE

            // Start shimmer animation
            shimmerAdapter.startShimmer()
            // Double the shimmer animation
            shimmerAdapter.startShimmer()

            // Start shimmer animation
            shimmerAdapter.startShimmer()
            // Start shimmer animation
            shimmerAdapter.startShimmer()

            // Start shimmer animation
            shimmerAdapter.startShimmer()
        } else {
            // Hide shimmer, show appropriate content
            recyclerViewShimmer.visibility = android.view.View.GONE
            shimmerAdapter.stopShimmer()

            // Show either list or empty state based on data
            if (adapter.itemCount > 0) {
                recyclerView.visibility = android.view.View.VISIBLE
                emptyStateView.visibility = android.view.View.GONE
            } else {
                recyclerView.visibility = android.view.View.GONE
                emptyStateView.visibility = android.view.View.VISIBLE
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
                    1 -> student.name.contains(query, ignoreCase = true)
                    2 -> student.course.contains(query, ignoreCase = true)
                    3 -> student.regNumber.contains(query, ignoreCase = true)
                    else -> student.name.contains(query, ignoreCase = true) ||
                            student.regNumber.contains(query, ignoreCase = true) ||
                            student.course.contains(query, ignoreCase = true) ||
                            student.email.contains(query, ignoreCase = true)
                }
            }
        }

        adapter.updateStudents(filteredStudents)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val hasStudents = adapter.itemCount > 0
        val hasSearchQuery = searchView.query.toString().isNotEmpty()

        if (!hasStudents) {
            if (hasSearchQuery) {
                showSearchEmptyState()
            } else {
                showNoStudentsEmptyState()
            }
            emptyStateView.visibility = android.view.View.VISIBLE
            recyclerView.visibility = android.view.View.GONE
        } else {
            emptyStateView.visibility = android.view.View.GONE
            recyclerView.visibility = android.view.View.VISIBLE
        }

        // Always hide shimmer when updating empty state
        recyclerViewShimmer.visibility = android.view.View.GONE
    }

    private fun showNoStudentsEmptyState() {
        ivEmptyIllustration.setImageResource(R.drawable.ic_empty_students)
        tvEmptyTitle.text = "No Students Yet"
        tvEmptySubtitle.text = "Get started by adding your first student to the system"
        btnAddFirstStudent.visibility = android.view.View.VISIBLE

        emptyStateView.alpha = 0f
        emptyStateView.animate()
            .alpha(1f)
            .setDuration(600)
            .start()
    }

    private fun showSearchEmptyState() {
        val query = searchView.query.toString()
        ivEmptyIllustration.setImageResource(R.drawable.ic_search_empty)
        tvEmptyTitle.text = "No Results Found"
        tvEmptySubtitle.text = "No students match \"$query\"\nTry different keywords or check the spelling"
        btnAddFirstStudent.visibility = android.view.View.GONE

        emptyStateView.alpha = 0f
        emptyStateView.animate()
            .alpha(1f)
            .setDuration(400)
            .start()
    }

    private fun syncWithCloud() {
        if (!firebaseManager.isUserLoggedIn()) {
            showToast("Please login to sync with cloud")
            return
        }

        showShimmerLoading(true)
        tvSyncStatus.text = "Syncing with cloud..."

        lifecycleScope.launch {
            when (val result = studentRepository.syncAllData()) {
                is Resource.Success -> {
                    showShimmerLoading(false)
                    tvSyncStatus.text = "Last sync: Just now"
                    showToast("Sync completed successfully! ✅")
                    loadStudents()
                }
                is Resource.Error -> {
                    showShimmerLoading(false)
                    tvSyncStatus.text = "Sync failed"
                    showToast("Sync failed: ${result.message}")
                }
                is Resource.Loading -> {
                    // Loading state handled by showShimmerLoading(true)
                }
            }
        }
    }

    private fun showStudentDetails(student: Student) {
        val details = """
            📝 First_Name: ${student.name}
            📝 Second_Name: ${student.name}
            🔢 Reg Number: ${student.regNumber}
            🎓 Course: ${student.course}
            📧 Email: ${student.email.ifEmpty { "Not provided" }}
            📞 Phone: ${student.phone.ifEmpty { "Not provided" }}
            ${if (student.isSynced) "✅ Synced to cloud" else "🔄 Local only"}
        """.trimIndent()

        val intent = Intent(this, StudentProfileActivity::class.java)
        intent.putExtra(StudentProfileActivity.EXTRA_STUDENT_ID, student.id)
        startActivity(intent)
    }

    private fun deleteStudent(student: Student) {
        AlertDialog.Builder(this)
            .setTitle("Delete Student")
            .setMessage("Are you sure you want to delete ${student.name}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    when (val result = studentRepository.deleteStudent(student.id)) {
                        is Resource.Success -> {
                            loadStudents()
                            showToast("${student.name} deleted successfully 🗑️")
                        }
                        is Resource.Error -> {
                            showToast("Failed to delete student: ${result.message}")
                        }
                        is Resource.Loading -> {
                            // Show loading if needed
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}