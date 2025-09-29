package com.smisapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
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

        // Add these for better animations
        val cardView: View = itemView.findViewById(R.id.cardView) // Make sure you have this in your item layout
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
            holder.ivSyncStatus.alpha = 1.0f
        } else {
            holder.ivSyncStatus.setImageResource(R.drawable.ic_cloud_off)
            holder.ivSyncStatus.contentDescription = "Not synced"
            holder.ivSyncStatus.alpha = 0.6f
        }

        // Load student photo with crossfade
        if (student.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(student.photoUrl)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .dontAnimate() // We'll handle animations separately
                .into(holder.ivStudentPhoto)
            holder.ivStudentPhoto.isVisible = true
        } else {
            holder.ivStudentPhoto.setImageResource(R.drawable.ic_person_placeholder)
            holder.ivStudentPhoto.isVisible = true
        }

        // Add click animation
        holder.itemView.setOnClickListener {
            // Scale animation on click
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    onItemClick(student)
                }
                .start()
        }

        // Add entrance animation for items (staggered)
        if (position > lastAnimatedPosition) {
            holder.itemView.alpha = 0f
            holder.itemView.translationY = 30f
            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay((position * 50).toLong())
                .start()
            lastAnimatedPosition = position
        }
    }

    override fun getItemCount() = students.size

    // Animation helper
    private var lastAnimatedPosition = -1

    fun updateStudents(newStudents: List<Student>) {
        val diffResult = DiffUtil.calculateDiff(StudentDiffCallback(students, newStudents))
        students = newStudents
        diffResult.dispatchUpdatesTo(this)

        // Reset animation position when data set completely changes
        if (newStudents.isEmpty() || students.size != newStudents.size) {
            lastAnimatedPosition = -1
        }
    }

    // DiffUtil for smooth animations during updates
    private class StudentDiffCallback(
        private val oldList: List<Student>,
        private val newList: List<Student>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}