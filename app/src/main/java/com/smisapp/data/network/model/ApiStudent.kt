package com.smisapp.data.network.model

import com.google.gson.annotations.SerializedName
import java.util.*

data class ApiStudent(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("regNumber")
    val regNumber: String,

    @SerializedName("course")
    val course: String,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("photoUrl")
    val photoUrl: String? = null,

    @SerializedName("createdAt")
    val createdAt: Long,

    @SerializedName("updatedAt")
    val updatedAt: Long,

    @SerializedName("isSynced")
    val isSynced: Boolean = true // API students are always synced
) {
    // Helper method to convert to local Student entity
    fun toStudent(): com.smisapp.data.entity.Student {
        return com.smisapp.data.entity.Student(
            id = this.id,
            name = this.name,
            regNumber = this.regNumber,
            course = this.course,
            email = this.email ?: "",
            phone = this.phone ?: "",
            photoUrl = this.photoUrl ?: "",
            firebaseId = this.id, // Use API ID as firebaseId for sync tracking
            isSynced = true,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}

// Request model for creating/updating students
data class StudentRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("regNumber")
    val regNumber: String,

    @SerializedName("course")
    val course: String,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("photoUrl")
    val photoUrl: String? = null
) {
    companion object {
        fun fromStudent(student: com.smisapp.data.entity.Student): StudentRequest {
            return StudentRequest(
                name = student.name,
                regNumber = student.regNumber,
                course = student.course,
                email = student.email.ifEmpty { null },
                phone = student.phone.ifEmpty { null },
                photoUrl = student.photoUrl.ifEmpty { null }
            )
        }
    }
}

// API Response wrapper
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: String? = null
)