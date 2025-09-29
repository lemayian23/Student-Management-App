package com.smisapp.data.network

import com.smisapp.data.entity.Student

class FirebaseManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: FirebaseManager? = null

        fun getInstance(): FirebaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseManager().also { INSTANCE = it }
            }
        }
    }

    // Authentication methods - return false for now
    suspend fun signIn(email: String, password: String): Boolean {
        return false // Temporarily disabled
    }

    suspend fun signUp(email: String, password: String): Boolean {
        return false // Temporarily disabled
    }

    fun isUserLoggedIn(): Boolean = false // Temporarily disabled

    fun getCurrentUserId(): String? = null // Temporarily disabled

    fun signOut() {
        // Do nothing for now
    }

    // Student data sync methods - return empty values for now
    suspend fun syncStudentToCloud(student: Student): String {
        return "" // Temporarily disabled
    }

    suspend fun getStudentsFromCloud(): List<Student> {
        return emptyList() // Temporarily disabled
    }

    suspend fun deleteStudentFromCloud(firebaseId: String): Boolean {
        return false // Temporarily disabled
    }

    // File upload - return empty string for now
    suspend fun uploadStudentPhoto(photoBytes: ByteArray, studentId: String): String {
        return "" // Temporarily disabled
    }

    // Sync all - return false for now
    suspend fun syncAllStudentsToCloud(students: List<Student>): Boolean {
        return false // Temporarily disabled
    }

    // Pull from cloud - return empty list for now
    suspend fun pullStudentsFromCloud(): List<Student> {
        return emptyList() // Temporarily disabled
    }
}