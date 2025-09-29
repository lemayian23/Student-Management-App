package com.smisapp.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smisapp.R
import com.smisapp.SMISApplication
import com.smisapp.data.entity.Student
import com.smisapp.data.repository.StudentRepository
import com.smisapp.data.network.FirebaseManager
import kotlinx.coroutines.launch
import java.util.*

class RegisterStudentActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etRegNumber: EditText
    private lateinit var etCourse: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var ivStudentPhoto: ImageView
    private lateinit var btnTakePhoto: Button
    private lateinit var btnRegister: Button
    private lateinit var switchCloudSync: Switch
    private lateinit var progressBar: ProgressBar

    private lateinit var studentRepository: StudentRepository
    private lateinit var firebaseManager: FirebaseManager

    private var selectedImageUri: Uri? = null
    private var photoBytes: ByteArray? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_student)

        initializeViews()
        setupRepository()
        setupClickListeners()
    }

    private fun initializeViews() {
        etName = findViewById(R.id.etStudentName)
        etRegNumber = findViewById(R.id.etRegNumber)
        etCourse = findViewById(R.id.etCourse)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        ivStudentPhoto = findViewById(R.id.ivStudentPhoto)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnRegister = findViewById(R.id.btnRegister)
        switchCloudSync = findViewById(R.id.switchCloudSync)
        progressBar = findViewById(R.id.progressBar)

        firebaseManager = FirebaseManager.getInstance()

        // Enable cloud sync only if user is logged in
        switchCloudSync.isEnabled = firebaseManager.isUserLoggedIn()
        if (!firebaseManager.isUserLoggedIn()) {
            switchCloudSync.isChecked = false
            switchCloudSync.text = "Cloud Sync (Login Required)"
        }
    }

    private fun setupRepository() {
        val database = (application as SMISApplication).database
        studentRepository = StudentRepository(
            database.studentDao(),
            firebaseManager
        )
    }

    private fun setupClickListeners() {
        btnTakePhoto.setOnClickListener {
            selectImage()
        }

        btnRegister.setOnClickListener {
            registerStudent()
        }

        ivStudentPhoto.setOnClickListener {
            selectImage()
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

    private fun registerStudent() {
        val name = etName.text.toString().trim()
        val regNumber = etRegNumber.text.toString().trim()
        val course = etCourse.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        if (name.isEmpty() || regNumber.isEmpty() || course.isEmpty()) {
            Toast.makeText(this, "Please fill required fields (Name, Reg Number, Course)", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidEmail(email) && email.isNotEmpty()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                // Create student object
                val student = Student(
                    name = name,
                    regNumber = regNumber,
                    course = course,
                    email = email,
                    phone = phone,
                    photoUrl = "", // Will be set after photo upload
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isSynced = !switchCloudSync.isChecked, // If cloud sync is off, mark as synced
                    firebaseId = ""
                )

                // Add student to repository
                val studentId = studentRepository.addStudent(student)

                // Upload photo if selected
                if (photoBytes != null && switchCloudSync.isChecked) {
                    val photoUrl = firebaseManager.uploadStudentPhoto(photoBytes!!, studentId)
                    if (photoUrl.isNotEmpty()) {
                        // Update student with photo URL
                        val updatedStudent = student.copy(photoUrl = photoUrl)
                        studentRepository.updateStudent(updatedStudent)
                    }
                }

                showLoading(false)
                Toast.makeText(this@RegisterStudentActivity, "Student registered successfully!", Toast.LENGTH_SHORT).show()
                clearForm()

            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@RegisterStudentActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnRegister.isEnabled = !show
    }

    private fun clearForm() {
        etName.text.clear()
        etRegNumber.text.clear()
        etCourse.text.clear()
        etEmail.text.clear()
        etPhone.text.clear()
        ivStudentPhoto.setImageResource(R.drawable.ic_person_placeholder)
        selectedImageUri = null
        photoBytes = null
    }
}