package com.smisapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smisapp.R
import com.smisapp.data.entity.Student

class StudentAdapter(
    private var students: List<Student>,
    private val onItemClick: (Student) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvStudentName)
        val tvReg: TextView = itemView.findViewById(R.id.tvRegNumber)
        val tvCourse: TextView = itemView.findViewById(R.id.tvCourse)
        val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        val ivSyncStatus: ImageView = itemView.findViewById(R.id.ivSyncStatus)
        val ivStudentPhoto: ImageView = itemView.findViewById(R.id.ivStudentPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_enhanced, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]

        holder.tvName.text = student.name
        holder.tvReg.text = student.regNumber
        holder.tvCourse.text = student.course
        holder.tvEmail.text = student.email.ifEmpty { "No email" }

        // Set sync status icon
        if (student.isSynced) {
            holder.ivSyncStatus.setImageResource(R.drawable.ic_cloud_done)
            holder.ivSyncStatus.contentDescription = "Synced to cloud"
        } else {
            holder.ivSyncStatus.setImageResource(R.drawable.ic_cloud_off)
            holder.ivSyncStatus.contentDescription = "Not synced"
        }

        // Load student photo
        if (student.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(student.photoUrl)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .into(holder.ivStudentPhoto)
        } else {
            holder.ivStudentPhoto.setImageResource(R.drawable.ic_person_placeholder)
        }

        holder.itemView.setOnClickListener {
            onItemClick(student)
        }
    }

    override fun getItemCount() = students.size

    fun updateStudents(newStudents: List<Student>) {
        students = newStudents
        notifyDataSetChanged()
    }
}