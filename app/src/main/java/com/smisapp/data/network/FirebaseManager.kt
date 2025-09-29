package com.smisapp.data.network

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.auth.auth
import com.google.firebase.storage.storage
import com.smisapp.data.entity.Student
import kotlinx.coroutines.tasks.await
import java.util.UUID

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

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val storage = Firebase.storage

    // Authentication methods
    suspend fun signIn(email: String, password: String): Boolean {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user != null
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Sign in failed: ${e.message}")
            false
        }
    }

    suspend fun signUp(email: String, password: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user != null
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Sign up failed: ${e.message}")
            false
        }
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun signOut() {
        auth.signOut()
    }

    // Student data sync methods
    suspend fun syncStudentToCloud(student: Student): String {
        return try {
            val studentData = student.toMap().toMutableMap()
            val firebaseId = if (student.firebaseId.isEmpty()) {
                UUID.randomUUID().toString()
            } else {
                student.firebaseId
            }

            studentData["firebaseId"] = firebaseId
            studentData["userId"] = getCurrentUserId() ?: "unknown"

            db.collection("students")
                .document(firebaseId)
                .set(studentData)
                .await()

            firebaseId
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Sync student failed: ${e.message}")
            ""
        }
    }

    suspend fun getStudentsFromCloud(): List<Student> {
        return try {
            val userId = getCurrentUserId() ?: return emptyList()

            val result = db.collection("students")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            result.documents.map { document ->
                Student.fromMap(document.data!!)
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Get students failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun deleteStudentFromCloud(firebaseId: String): Boolean {
        return try {
            db.collection("students")
                .document(firebaseId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Delete student failed: ${e.message}")
            false
        }
    }

    // File upload for student photos
    suspend fun uploadStudentPhoto(photoBytes: ByteArray, studentId: String): String {
        return try {
            val storageRef = storage.reference
            val photoRef = storageRef.child("student_photos/$studentId.jpg")

            val uploadTask = photoRef.putBytes(photoBytes).await()
            val downloadUrl = photoRef.downloadUrl.await()

            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Photo upload failed: ${e.message}")
            ""
        }
    }

    // Sync all local data to cloud
    suspend fun syncAllStudentsToCloud(students: List<Student>): Boolean {
        return try {
            students.forEach { student ->
                if (!student.isSynced) {
                    val firebaseId = syncStudentToCloud(student)
                    if (firebaseId.isNotEmpty()) {
                        // Mark as synced in local DB (you'll update this later)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Bulk sync failed: ${e.message}")
            false
        }
    }

    // Pull cloud data to local
    suspend fun pullStudentsFromCloud(): List<Student> {
        return getStudentsFromCloud()
    }
}