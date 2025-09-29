package com.smisapp.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

class StudentItemAnimator : DefaultItemAnimator() {

    init {
        // Configure animation durations
        addDuration = 300L
        removeDuration = 300L
        moveDuration = 300L
        changeDuration = 300L
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
        if (holder != null) {
            // Slide in from bottom with fade
            val view = holder.itemView
            view.translationY = view.height.toFloat()
            view.alpha = 0f

            val animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f)
            val alphaAnimator = ObjectAnimator.ofFloat(view, View.ALPHA, 1f)

            animator.duration = addDuration
            animator.interpolator = AccelerateDecelerateInterpolator()
            alphaAnimator.duration = addDuration

            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    dispatchAddFinished(holder)
                }
            })

            animator.start()
            alphaAnimator.start()
            return false
        }
        return super.animateAdd(holder)
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder?): Boolean {
        if (holder != null) {
            // Slide out to right with fade
            val view = holder.itemView

            val animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, view.width.toFloat())
            val alphaAnimator = ObjectAnimator.ofFloat(view, View.ALPHA, 0f)

            animator.duration = removeDuration
            animator.interpolator = AccelerateDecelerateInterpolator()
            alphaAnimator.duration = removeDuration

            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    dispatchRemoveFinished(holder)
                }
            })

            animator.start()
            alphaAnimator.start()
            return false
        }
        return super.animateRemove(holder)
    }

    override fun animateMove(
        holder: RecyclerView.ViewHolder?,
        fromX: Int, fromY: Int,
        toX: Int, toY: Int
    ): Boolean {
        // Use default move animation
        return super.animateMove(holder, fromX, fromY, toX, toY)
    }

    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder?,
        newHolder: RecyclerView.ViewHolder?,
        fromLeft: Int, fromTop: Int,
        toLeft: Int, toTop: Int
    ): Boolean {
        // Use default change animation
        return super.animateChange(oldHolder, newHolder, fromLeft, fromTop, toLeft, toTop)
    }
}