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
    private val onItemClick: (Student) -> Unit,
    private val onMoreActionsClick: (Student, View) -> Unit = { _, _ -> } // New callback for actions
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Core information
        val tvName: TextView = itemView.findViewById(R.id.tvStudentName)
        val tvReg: TextView = itemView.findViewById(R.id.tvRegNumber)
        val tvCourse: TextView = itemView.findViewById(R.id.tvCourse)
        val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)

        // Images
        val ivStudentPhoto: ImageView = itemView.findViewById(R.id.ivStudentPhoto)
        val ivSyncStatus: ImageView = itemView.findViewById(R.id.ivSyncStatus)
        val ivMoreActions: ImageView = itemView.findViewById(R.id.ivMoreActions)

        // FIXED: Use the actual emailContainer ID instead of parent casting
        val emailContainer: ViewGroup = itemView.findViewById(R.id.emailContainer)

        // Card for animations
        val cardView: View = itemView.findViewById(R.id.cardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_enhanced, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]

        // Set student information
        holder.tvName.text = student.name
        holder.tvReg.text = student.regNumber
        holder.tvCourse.text = student.course
        holder.tvEmail.text = student.email.ifEmpty { "No email" }

        // Show/hide email based on availability
        holder.emailContainer.isVisible = student.email.isNotEmpty()

        // Set sync status with enhanced visuals
        if (student.isSynced) {
            holder.ivSyncStatus.setImageResource(R.drawable.ic_cloud_done)
            holder.ivSyncStatus.contentDescription = "Synced to cloud"
            holder.ivSyncStatus.alpha = 1.0f
            // You could add a tint here for better visual feedback
            // holder.ivSyncStatus.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.sync_success))
        } else {
            holder.ivSyncStatus.setImageResource(R.drawable.ic_cloud_off)
            holder.ivSyncStatus.contentDescription = "Not synced - local only"
            holder.ivSyncStatus.alpha = 0.7f
            // holder.ivSyncStatus.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.sync_warning))
        }

        // Load student photo with enhanced styling
        if (student.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(student.photoUrl)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .circleCrop() // Enhanced: Use circle crop for better avatar appearance
                .into(holder.ivStudentPhoto)
        } else {
            holder.ivStudentPhoto.setImageResource(R.drawable.ic_person_placeholder)
        }

        // Setup click listeners
        setupClickListeners(holder, student)

        // Add entrance animation for items (staggered)
        addEntranceAnimation(holder, position)
    }

    private fun setupClickListeners(holder: StudentViewHolder, student: Student) {
        // Main card click - with enhanced animation
        holder.cardView.setOnClickListener {
            // Enhanced scale animation with depth effect
            it.animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .translationZ(-8f)
                .setDuration(80)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationZ(0f)
                        .setDuration(120)
                        .start()
                    onItemClick(student)
                }
                .start()
        }

        // More actions click - with ripple feedback
        holder.ivMoreActions.setOnClickListener { view ->
            // Scale animation for the icon
            view.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(100)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    onMoreActionsClick(student, view)
                }
                .start()
        }

        // Long press support for future features
        holder.cardView.setOnLongClickListener {
            // You could add haptic feedback here
            // it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            true // Return true to indicate the event was handled
        }
    }

    private fun addEntranceAnimation(holder: StudentViewHolder, position: Int) {
        if (position > lastAnimatedPosition) {
            holder.itemView.alpha = 0f
            holder.itemView.translationY = 40f
            holder.itemView.scaleX = 0.95f
            holder.itemView.scaleY = 0.95f

            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay((position * 60).toLong())
                .setInterpolator(android.view.animation.DecelerateInterpolator())
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
            val oldStudent = oldList[oldItemPosition]
            val newStudent = newList[newItemPosition]

            return oldStudent.name == newStudent.name &&
                    oldStudent.regNumber == newStudent.regNumber &&
                    oldStudent.course == newStudent.course &&
                    oldStudent.email == newStudent.email &&
                    oldStudent.photoUrl == newStudent.photoUrl &&
                    oldStudent.isSynced == newStudent.isSynced
        }

        // Optional: For fine-grained update control
        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            // You can return specific change payloads here for partial updates
            return null
        }
    }
}