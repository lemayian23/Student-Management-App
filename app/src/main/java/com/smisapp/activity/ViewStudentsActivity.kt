package com.smisapp.activity

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smisapp.R
import com.smisapp.adapter.StudentAdapter
import com.smisapp.data.database.StudentDatabase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class ViewStudentsActivity : AppCompatActivity() {

    private lateinit var db: StudentDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_students)

        db = StudentDatabase.getInstance(this)
        recyclerView = findViewById(R.id.recyclerViewStudents)
        tvEmpty = findViewById(R.id.tvEmptyMessage)

        recyclerView.layoutManager = LinearLayoutManager(this)
        loadStudents()
    }

    private fun loadStudents() {
        lifecycleScope.launch {
            db.studentDao().getAllStudents().collect { students ->
                if (students.isEmpty()) {
                    tvEmpty.text = "No students registered yet"
                    tvEmpty.visibility = TextView.VISIBLE
                    recyclerView.visibility = RecyclerView.GONE
                } else {
                    tvEmpty.visibility = TextView.GONE
                    recyclerView.visibility = RecyclerView.VISIBLE
                    val adapter = StudentAdapter(students)
                    recyclerView.adapter = adapter
                }
            }
        }
    }
}