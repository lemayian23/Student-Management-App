package com.smisapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smisapp.R

class ShimmerStudentAdapter : RecyclerView.Adapter<ShimmerStudentAdapter.ShimmerViewHolder>() {

    class ShimmerViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShimmerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_shimmer, parent, false)
        return ShimmerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShimmerViewHolder, position: Int) {
        // No binding needed for shimmer items
    }

    override fun getItemCount(): Int = 6 // Show 6 shimmer items

    fun startShimmer() {
        // Shimmer starts automatically via XML attribute
    }

    fun stopShimmer() {
        // Shimmer will stop when view is detached
    }
}