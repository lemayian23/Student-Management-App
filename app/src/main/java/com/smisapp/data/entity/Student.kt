package com.smisapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index
import java.util.Date

@Entity(
    tableName = "students",
    indices = [Index(value = ["reg_number"], unique = true)]
)
data class Student(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = "", // Changed to String for Firebase compatibility

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "reg_number")
    val regNumber: String,

    @ColumnInfo(name = "course")
    val course: String,

    // New fields for enhanced features
    @ColumnInfo(name = "email")
    val email: String = "",

    @ColumnInfo(name = "phone")
    val phone: String = "",

    @ColumnInfo(name = "photo_url")
    val photoUrl: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "firebase_id")
    val firebaseId: String = ""
) {
    // Helper method to convert to Map for Firebase
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "regNumber" to regNumber,
            "course" to course,
            "email" to email,
            "phone" to phone,
            "photoUrl" to photoUrl,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "isSynced" to isSynced
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Student {
            return Student(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                regNumber = map["regNumber"] as? String ?: "",
                course = map["course"] as? String ?: "",
                email = map["email"] as? String ?: "",
                phone = map["phone"] as? String ?: "",
                photoUrl = map["photoUrl"] as? String ?: "",
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Long) ?: System.currentTimeMillis(),
                isSynced = map["isSynced"] as? Boolean ?: true,
                firebaseId = map["firebaseId"] as? String ?: ""
            )
        }
    }
}