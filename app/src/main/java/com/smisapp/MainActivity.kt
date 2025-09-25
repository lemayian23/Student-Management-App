package com.smisapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smisapp.activity.RegisterStudentActivity
import com.smisapp.activity.ViewStudentsActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            val btnRegister = findViewById<Button>(R.id.btnRegisterStudent)
            val btnView = findViewById<Button>(R.id.btnViewStudents)
            val btnReport = findViewById<Button>(R.id.btnGenerateReport)

            btnRegister.setOnClickListener {
                try {
                    startActivity(Intent(this, RegisterStudentActivity::class.java))
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            btnView.setOnClickListener {
                try {
                    startActivity(Intent(this, ViewStudentsActivity::class.java))
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            btnReport.setOnClickListener {
                Toast.makeText(this, "Reports coming soon", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "App error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}