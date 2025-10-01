package com.smisapp.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.smisapp.R
import com.smisapp.SMISApplication
import com.smisapp.data.entity.Student
import com.smisapp.data.repository.Resource
import com.smisapp.data.repository.StudentRepository
import com.smisapp.data.network.FirebaseManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StudentProfileActivity : AppCompatActivity() {

    private lateinit var studentRepository: StudentRepository
    private lateinit var firebaseManager: FirebaseManager

    // Views
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var tvStudentName: TextView
    private lateinit var tvRegNumber: TextView
    private lateinit var tvCourse: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvDateOfBirth: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvEmergencyContact: TextView
    private lateinit var tvNotes: TextView
    private lateinit var tvSyncStatus: TextView
    private lateinit var tvCreatedAt: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnCall: ImageButton
    private lateinit var btnEmail: ImageButton
    private lateinit var btnShare: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout

    private var currentStudent: Student? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_profile)

        initializeViews()
        setupRepository()
        loadStudentData()
        setupClickListeners()
    }

    private fun initializeViews() {
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)
        tvStudentName = findViewById(R.id.tvStudentName)
        tvRegNumber = findViewById(R.id.tvRegNumber)
        tvCourse = findViewById(R.id.tvCourse)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvAddress = findViewById(R.id.tvAddress)
        tvDateOfBirth = findViewById(R.id.tvDateOfBirth)
        tvGender = findViewById(R.id.tvGender)
        tvEmergencyContact = findViewById(R.id.tvEmergencyContact)
        tvNotes = findViewById(R.id.tvNotes)
        tvSyncStatus = findViewById(R.id.tvSyncStatus)
        tvCreatedAt = findViewById(R.id.tvCreatedAt)
        btnEdit = findViewById(R.id.btnEdit)
        btnCall = findViewById(R.id.btnCall)
        btnEmail = findViewById(R.id.btnEmail)
        btnShare = findViewById(R.id.btnShare)
        progressBar = findViewById(R.id.progressBar)
        contentLayout = findViewById(R.id.contentLayout)

        firebaseManager = FirebaseManager.getInstance()
    }

    private fun setupRepository() {
        val database = (application as SMISApplication).database
        studentRepository = StudentRepository(
            database.studentDao(),
            firebaseManager
        )
    }

    private fun loadStudentData() {
        val studentId = intent.getStringExtra(EXTRA_STUDENT_ID)
        if (studentId.isNullOrEmpty()) {
            showError("Student ID not provided")
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            when (val result = studentRepository.getStudentById(studentId)) {
                is Resource.Success -> {
                    currentStudent = result.data
                    if (currentStudent != null) {
                        populateStudentData(currentStudent!!)
                        showLoading(false)
                    } else {
                        showError("Student not found")
                    }
                }
                is Resource.Error -> {
                    showError("Failed to load student: ${result.message}")
                }
                is Resource.Loading -> {
                    // Loading state handled by showLoading(true)
                }
            }
        }
    }

    private fun populateStudentData(student: Student) {
        // Basic Information
        tvStudentName.text = student.name
        tvRegNumber.text = student.regNumber
        tvCourse.text = student.course

        // Contact Information
        tvEmail.text = student.email.ifEmpty { "Not provided" }
        tvPhone.text = student.phone.ifEmpty { "Not provided" }
        tvAddress.text = student.address.ifEmpty { "Not provided" }

        // Personal Information
        tvDateOfBirth.text = if (student.dateOfBirth > 0) {
            formatDate(student.dateOfBirth)
        } else {
            "Not provided"
        }

        tvGender.text = student.gender.ifEmpty { "Not provided" }
        tvEmergencyContact.text = student.emergencyContact.ifEmpty { "Not provided" }
        tvNotes.text = student.notes.ifEmpty { "No additional notes" }

        // System Information
        tvSyncStatus.text = if (student.isSynced) "✅ Synced to Cloud" else "🔄 Local Only"
        tvCreatedAt.text = "Registered: ${formatDate(student.createdAt)}"

        // Load profile photo
        if (student.photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(student.photoUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(ivProfilePhoto)
        } else {
            ivProfilePhoto.setImageResource(R.drawable.ic_profile_placeholder)
        }

        // Update button states based on available data
        updateActionButtons(student)
    }

    private fun setupClickListeners() {
        btnEdit.setOnClickListener {
            currentStudent?.let { student ->
                // Navigate to edit activity (we'll create this later)
                val intent = Intent(this, EditStudentActivity::class.java)
                intent.putExtra(EditStudentActivity.EXTRA_STUDENT_ID, student.id)
                startActivity(intent)
            }
        }

        btnCall.setOnClickListener {
            currentStudent?.let { student ->
                if (student.phone.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse("tel:${student.phone}")
                    startActivity(intent)
                } else {
                    showToast("Phone number not available")
                }
            }
        }

        btnEmail.setOnClickListener {
            currentStudent?.let { student ->
                if (student.email.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = Uri.parse("mailto:${student.email}")
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding ${student.name} - ${student.regNumber}")
                    startActivity(intent)
                } else {
                    showToast("Email not available")
                }
            }
        }

        btnShare.setOnClickListener {
            currentStudent?.let { student ->
                val shareText = """
                    📋 Student Information:
                    
                    👤 Name: ${student.name}
                    🔢 Registration: ${student.regNumber}
                    🎓 Course: ${student.course}
                    📧 Email: ${student.email}
                    📞 Phone: ${student.phone}
                    
                    Shared via SMIS App
                """.trimIndent()

                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, shareText)
                startActivity(Intent.createChooser(intent, "Share Student Information"))
            }
        }
    }

    private fun updateActionButtons(student: Student) {
        // Enable/disable buttons based on available data
        btnCall.isEnabled = student.phone.isNotEmpty()
        btnEmail.isEnabled = student.email.isNotEmpty()

        // Visual feedback for disabled buttons
        if (!btnCall.isEnabled) {
            btnCall.alpha = 0.5f
        }
        if (!btnEmail.isEnabled) {
            btnEmail.alpha = 0.5f
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        contentLayout.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        showLoading(false)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    companion object {
        const val EXTRA_STUDENT_ID = "extra_student_id"
    }
}