package com.smisapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "students")
data class Student(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Basic information
    val name: String,
    val regNumber: String,
    val course: String,
    val email: String,
    val phone: String = "",

    // Cloud sync fields
    val firebaseId: String = "",
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),

    // Additional fields for enhanced features
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),

    // Optional fields for future expansion
    val address: String = "",
    val dateOfBirth: Long = 0L,
    val gender: String = "",
    val emergencyContact: String = "",
    val notes: String = ""
) {

    /**
     * Validation helper methods
     */
    fun isValid(): Boolean {
        return name.isNotBlank() && regNumber.isNotBlank() && course.isNotBlank()
    }

    fun hasMissingRequiredFields(): Boolean {
        return name.isBlank() || regNumber.isBlank() || course.isBlank()
    }

    /**
     * Copy with sync update
     */
    fun markAsSynced(firebaseId: String): Student {
        return this.copy(
            firebaseId = firebaseId,
            isSynced = true,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * For search and display
     */
    fun getDisplayInfo(): String {
        return "$name • $regNumber • $course"
    }
}