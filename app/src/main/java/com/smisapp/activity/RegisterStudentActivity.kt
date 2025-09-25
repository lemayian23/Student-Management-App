package com.smisapp.activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smisapp.R
import com.smisapp.data.database.StudentDatabase
import com.smisapp.data.entity.Student
import kotlinx.coroutines.launch

class RegisterStudentActivity : AppCompatActivity() {

    private lateinit var db: StudentDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_student)

        db = StudentDatabase.getInstance(this)

        val etName = findViewById<EditText>(R.id.etStudentName)
        val etReg = findViewById<EditText>(R.id.etRegNumber)
        val etCourse = findViewById<EditText>(R.id.etCourse)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val name = etName.text.toString()
            val reg = etReg.text.toString()
            val course = etCourse.text.toString()

            if (name.isEmpty() || reg.isEmpty() || course.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val student = Student(name = name, regNumber = reg, course = course)

                lifecycleScope.launch {
                    db.studentDao().insertStudent(student)
                    runOnUiThread {
                        Toast.makeText(this@RegisterStudentActivity, "Student Registered!", Toast.LENGTH_SHORT).show()
                        etName.text.clear()
                        etReg.text.clear()
                        etCourse.text.clear()
                    }
                }
            }
        }
    }
}