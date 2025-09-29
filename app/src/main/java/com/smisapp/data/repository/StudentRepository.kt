package com.smisapp.data.repository

import com.smisapp.data.dao.StudentDao
import com.smisapp.data.entity.Student
import com.smisapp.data.network.FirebaseManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class StudentRepository(
    private val studentDao: StudentDao,
    private val firebaseManager: FirebaseManager
) {

    // Get all students with automatic cloud sync
    fun getAllStudents(): Flow<List<Student>> {
        return studentDao.getAllStudents().map { localStudents ->
            // Sync with cloud in background
            syncWithCloud(localStudents)
            localStudents
        }
    }

    // Add student with automatic cloud sync
    suspend fun addStudent(student: Student): String {
        // Generate unique ID if not provided
        val studentWithId = if (student.id.isEmpty()) {
            student.copy(id = UUID.randomUUID().toString())
        } else {
            student
        }

        // Save to local database first
        studentDao.insertStudent(studentWithId)

        // Sync to cloud in background
        syncStudentToCloud(studentWithId)

        return studentWithId.id
    }

    // Update student with cloud sync
    suspend fun updateStudent(student: Student) {
        studentDao.updateStudent(student)
        syncStudentToCloud(student)
    }

    // Delete student with cloud sync
    suspend fun deleteStudent(studentId: String) {
        val student = studentDao.getStudentById(studentId)
        student?.let {
            // Delete from cloud first, then local
            if (it.firebaseId.isNotEmpty()) {
                firebaseManager.deleteStudentFromCloud(it.firebaseId)
            }
        }
        studentDao.deleteStudent(studentId)
    }

    // Get student by ID
    suspend fun getStudentById(studentId: String): Student? {
        return studentDao.getStudentById(studentId)
    }

    // Manual sync operations
    suspend fun syncAllData(): Boolean {
        return try {
            // Push all unsynced local data to cloud
            val unsyncedStudents = studentDao.getUnsyncedStudents()
            unsyncedStudents.forEach { student ->
                syncStudentToCloud(student)
            }

            // Pull latest data from cloud
            val cloudStudents = firebaseManager.pullStudentsFromCloud()
            cloudStudents.forEach { cloudStudent ->
                val localStudent = studentDao.getStudentByFirebaseId(cloudStudent.firebaseId)
                if (localStudent == null) {
                    // New student from cloud - add to local
                    studentDao.insertStudent(cloudStudent.copy(isSynced = true))
                } else if (cloudStudent.updatedAt > localStudent.updatedAt) {
                    // Cloud has newer version - update local
                    studentDao.updateStudent(cloudStudent.copy(isSynced = true))
                }
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    // Force sync from cloud to local
    suspend fun forceSyncFromCloud(): Boolean {
        return try {
            val cloudStudents = firebaseManager.pullStudentsFromCloud()

            // Clear local data and replace with cloud data
            studentDao.deleteAllStudents()

            cloudStudents.forEach { student ->
                studentDao.insertStudent(student.copy(isSynced = true))
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    // Search students - FIXED: Use .first() to get the list from Flow
    suspend fun searchStudents(query: String): List<Student> {
        val allStudents = studentDao.getAllStudents().first() // Get the actual list
        return allStudents.filter { student ->
            student.name.contains(query, ignoreCase = true) ||
                    student.regNumber.contains(query, ignoreCase = true) ||
                    student.course.contains(query, ignoreCase = true) ||
                    student.email.contains(query, ignoreCase = true)
        }
    }

    // Private helper methods
    private suspend fun syncWithCloud(localStudents: List<Student>) {
        // Background sync - temporarily disabled
        // if (firebaseManager.isUserLoggedIn()) {
        //     firebaseManager.syncAllStudentsToCloud(localStudents)
        // }
    }

    private suspend fun syncStudentToCloud(student: Student) {
        // Temporarily disabled
        // if (firebaseManager.isUserLoggedIn()) {
        //     val firebaseId = firebaseManager.syncStudentToCloud(student)
        //     if (firebaseId.isNotEmpty()) {
        //         studentDao.updateStudent(
        //             student.copy(
        //                 firebaseId = firebaseId,
        //                 isSynced = true,
        //                 updatedAt = System.currentTimeMillis()
        //             )
        //         )
        //     }
        // }
    }
}