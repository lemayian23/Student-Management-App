package com.smisapp.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.smisapp.R
import com.smisapp.SMISApplication
import com.smisapp.data.entity.Student
import com.smisapp.data.repository.Resource
import com.smisapp.data.repository.StudentRepository
import com.smisapp.data.network.FirebaseManager
import kotlinx.coroutines.launch

class EditStudentActivity : AppCompatActivity() {

    private lateinit var etName: com.google.android.material.textfield.TextInputEditText
    private lateinit var etRegNumber: com.google.android.material.textfield.TextInputEditText
    private lateinit var etCourse: com.google.android.material.textfield.TextInputEditText
    private lateinit var etEmail: com.google.android.material.textfield.TextInputEditText
    private lateinit var etPhone: com.google.android.material.textfield.TextInputEditText
    private lateinit var ivStudentPhoto: ImageView
    private lateinit var btnChangePhoto: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnCancel: Button
    private lateinit var tvSyncStatus: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var studentRepository: StudentRepository
    private lateinit var firebaseManager: FirebaseManager

    private var currentStudent: Student? = null
    private var selectedImageUri: Uri? = null
    private var photoBytes: ByteArray? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 200
        const val EXTRA_STUDENT_ID = "extra_student_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_student)

        initializeViews()
        setupRepository()
        setupClickListeners()
        loadStudentData()
    }

    private fun initializeViews() {
        etName = findViewById(R.id.etStudentName)
        etRegNumber = findViewById(R.id.etRegNumber)
        etCourse = findViewById(R.id.etCourse)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        ivStudentPhoto = findViewById(R.id.ivStudentPhoto)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnCancel = findViewById(R.id.btnCancel)
        tvSyncStatus = findViewById(R.id.tvSyncStatus)
        progressBar = findViewById(R.id.progressBar)

        firebaseManager = FirebaseManager.getInstance()
        updateSyncUI()
    }

    private fun setupRepository() {
        val database = (application as SMISApplication).database
        studentRepository = StudentRepository(
            database.studentDao(),
            firebaseManager
        )
    }

    private fun setupClickListeners() {
        btnChangePhoto.setOnClickListener {
            selectImage()
        }

        btnUpdate.setOnClickListener {
            updateStudent()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        ivStudentPhoto.setOnClickListener {
            selectImage()
        }
    }

    private fun loadStudentData() {
        val studentId = intent.getStringExtra(EXTRA_STUDENT_ID)
        if (studentId.isNullOrEmpty()) {
            showToast("Student data not found")
            finish()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            when (val result = studentRepository.getStudentById(studentId)) {
                is Resource.Success -> {
                    currentStudent = result.data
                    if (currentStudent != null) {
                        populateStudentData(currentStudent!!)
                    } else {
                        showToast("Student not found")
                        finish()
                    }
                    showLoading(false)
                }
                is Resource.Error -> {
                    showToast("Failed to load student: ${result.message}")
                    showLoading(false)
                    finish()
                }
                is Resource.Loading -> {
                    // Loading handled by showLoading
                }
            }
        }
    }

    private fun populateStudentData(student: Student) {
        etName.setText(student.name)
        etRegNumber.setText(student.regNumber)
        etCourse.setText(student.course)
        etEmail.setText(student.email)
        etPhone.setText(student.phone)

        // Load student photo
        if (student.photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(student.photoUrl)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(ivStudentPhoto)
        }

        // Update sync status
        updateSyncStatus(student.isSynced)
    }

    private fun updateSyncStatus(isSynced: Boolean) {
        if (isSynced) {
            tvSyncStatus.text = "Synced with cloud ✅"
        } else {
            tvSyncStatus.text = "Local only ⚠️"
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            ivStudentPhoto.setImageURI(selectedImageUri)

            // Convert image to bytes for Firebase storage
            selectedImageUri?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    photoBytes = inputStream?.readBytes()
                    inputStream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun updateStudent() {
        val name = etName.text.toString().trim()
        val regNumber = etRegNumber.text.toString().trim()
        val course = etCourse.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        if (name.isEmpty() || regNumber.isEmpty() || course.isEmpty()) {
            showToast("Please fill required fields (Name, Reg Number, Course)")
            return
        }

        if (!isValidEmail(email) && email.isNotEmpty()) {
            showToast("Please enter a valid email address")
            return
        }

        val currentStudent = currentStudent ?: run {
            showToast("Student data not loaded")
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                // Create updated student object
                val updatedStudent = currentStudent.copy(
                    name = name,
                    regNumber = regNumber,
                    course = course,
                    email = email,
                    phone = phone,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false // Mark as unsynced since we're updating
                )

                // Update student in repository
                when (val result = studentRepository.updateStudent(updatedStudent)) {
                    is Resource.Success -> {
                        // Upload new photo if selected
                        if (photoBytes != null) {
                            val photoUrl = firebaseManager.uploadStudentPhoto(photoBytes!!, updatedStudent.id)
                            if (photoUrl.isNotEmpty()) {
                                // Update student with new photo URL
                                val studentWithPhoto = updatedStudent.copy(photoUrl = photoUrl)
                                studentRepository.updateStudent(studentWithPhoto)
                            }
                        }

                        showLoading(false)
                        showToast("Student updated successfully! ✅")
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        showToast("Failed to update student: ${result.message}")
                    }
                    is Resource.Loading -> {
                        // Loading handled by showLoading
                    }
                }

            } catch (e: Exception) {
                showLoading(false)
                showToast("Unexpected error: ${e.message}")
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnUpdate.isEnabled = !show
        btnCancel.isEnabled = !show
        btnChangePhoto.isEnabled = !show
    }

    private fun updateSyncUI() {
        // You can add any sync-related UI updates here
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}