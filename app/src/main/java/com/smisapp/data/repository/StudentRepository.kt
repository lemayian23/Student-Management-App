package com.smisapp.data.repository

import com.smisapp.data.dao.StudentDao
import com.smisapp.data.entity.Student
import com.smisapp.data.network.FirebaseManager
import com.smisapp.data.network.NetworkManager
import com.smisapp.data.network.api.StudentApiService
import com.smisapp.data.network.model.StudentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

// ==================== ADVANCED PATTERNS: State Management ====================

/**
 * Resource wrapper for handling loading, success, and error states
 * This follows the Android recommended pattern for handling async operations
 */
sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
}

/**
 * Result wrapper for operations that don't need loading states
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Result<Nothing>()
}

// ==================== ENHANCED REPOSITORY WITH RETROFIT ====================

class StudentRepository(
    private val studentDao: StudentDao,
    private val firebaseManager: FirebaseManager,
    private val networkManager: NetworkManager = NetworkManager.getInstance()
) {

    private val studentApiService: StudentApiService = networkManager.getStudentApiService()

    // ==================== EXISTING FUNCTIONALITY (ENHANCED WITH RETROFIT) ====================

    // Get all students with automatic cloud sync AND API sync
    fun getAllStudents(): Flow<List<Student>> {
        return studentDao.getAllStudents().map { localStudents ->
            try {
                // Sync with cloud in background with error handling
                syncWithCloud(localStudents)

                // Sync with API in background if network available and no local data
                if (networkManager.isNetworkAvailable() && localStudents.isEmpty()) {
                    syncFromApi()
                }
            } catch (e: Exception) {
                // Log error but don't crash - local data is still available
                e.printStackTrace()
            }
            localStudents
        }
    }

    // Add student with automatic cloud sync AND API sync - NOW WITH RETROFIT
    suspend fun addStudent(student: Student): Resource<String> {
        return try {
            // Generate unique ID if not provided
            val studentWithId = if (student.id.isEmpty()) {
                student.copy(id = UUID.randomUUID().toString())
            } else {
                student
            }

            // Save to local database first (offline-first approach)
            studentDao.insertStudent(studentWithId)

            // Try to sync with API if network available
            if (networkManager.isNetworkAvailable()) {
                try {
                    val response = studentApiService.createStudent(
                        StudentRequest.fromStudent(studentWithId)
                    )

                    if (response.isSuccessful && response.body()?.success == true) {
                        val apiStudent = response.body()!!.data!!
                        // Update local student with API ID and mark as synced
                        val syncedStudent = studentWithId.copy(
                            firebaseId = apiStudent.id,
                            isSynced = true,
                            updatedAt = System.currentTimeMillis()
                        )
                        studentDao.updateStudent(syncedStudent)
                    }
                } catch (e: Exception) {
                    // API call failed, student remains locally
                    // We'll sync later when network is available
                    e.printStackTrace()
                }
            }

            // Sync to Firebase cloud in background
            syncStudentToCloud(studentWithId)

            Resource.Success(studentWithId.id)
        } catch (e: Exception) {
            Resource.Error("Failed to add student: ${e.message}", e)
        }
    }

    // Update student with cloud sync AND API sync - NOW WITH RETROFIT
    suspend fun updateStudent(student: Student): Resource<Unit> {
        return try {
            // Update local database first
            val updatedStudent = student.copy(updatedAt = System.currentTimeMillis())
            studentDao.updateStudent(updatedStudent)

            // Try to sync with API if network available and has API ID
            if (networkManager.isNetworkAvailable() && student.firebaseId.isNotEmpty()) {
                try {
                    val response = studentApiService.updateStudent(
                        student.firebaseId,
                        StudentRequest.fromStudent(updatedStudent)
                    )
                    // Mark as synced if API call succeeds
                    if (response.isSuccessful && response.body()?.success == true) {
                        studentDao.updateStudent(updatedStudent.copy(isSynced = true))
                    } else {
                        studentDao.updateStudent(updatedStudent.copy(isSynced = false))
                    }
                } catch (e: Exception) {
                    // Mark as unsynced if API call fails
                    studentDao.updateStudent(updatedStudent.copy(isSynced = false))
                }
            }

            // Sync to Firebase cloud
            syncStudentToCloud(updatedStudent)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to update student: ${e.message}", e)
        }
    }

    // Delete student with cloud sync AND API sync - NOW WITH RETROFIT
    suspend fun deleteStudent(studentId: String): Resource<Unit> {
        return try {
            val student = studentDao.getStudentById(studentId)

            student?.let {
                // Try to delete from API first if network available
                if (networkManager.isNetworkAvailable() && it.firebaseId.isNotEmpty()) {
                    try {
                        studentApiService.deleteStudent(it.firebaseId)
                    } catch (e: Exception) {
                        // API deletion failed, but we'll still delete locally
                        e.printStackTrace()
                    }
                }

                // Delete from Firebase cloud
                if (it.firebaseId.isNotEmpty()) {
                    firebaseManager.deleteStudentFromCloud(it.firebaseId)
                }
            }

            // Delete from local database
            studentDao.deleteStudent(studentId)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to delete student: ${e.message}", e)
        }
    }

    // Get student by ID - NOW WITH API FALLBACK
    suspend fun getStudentById(studentId: String): Resource<Student?> {
        return try {
            var student = studentDao.getStudentById(studentId)

            // If not found locally and network available, try API
            if (student == null && networkManager.isNetworkAvailable()) {
                try {
                    val response = studentApiService.getStudent(studentId)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val apiStudent = response.body()!!.data!!
                        student = apiStudent.toStudent()
                        // Save to local database for future offline access
                        studentDao.insertStudent(student!!)
                    }
                } catch (e: Exception) {
                    // API call failed, return null
                    e.printStackTrace()
                }
            }

            Resource.Success(student)
        } catch (e: Exception) {
            Resource.Error("Failed to get student: ${e.message}", e)
        }
    }

    // ==================== ENHANCED SYNC OPERATIONS WITH RETROFIT ====================

    // Manual sync operations - NOW WITH RETROFIT API SYNC
    suspend fun syncAllData(maxRetries: Int = 3): Resource<Boolean> {
        var lastException: Exception? = null

        // Retry mechanism
        for (attempt in 1..maxRetries) {
            try {
                // Push all unsynced local data to API
                if (networkManager.isNetworkAvailable()) {
                    val unsyncedStudents = studentDao.getUnsyncedStudents()
                    unsyncedStudents.forEach { student ->
                        try {
                            if (student.firebaseId.isEmpty()) {
                                // New student - create via API
                                val response = studentApiService.createStudent(
                                    StudentRequest.fromStudent(student)
                                )
                                if (response.isSuccessful && response.body()?.success == true) {
                                    val apiStudent = response.body()!!.data!!
                                    studentDao.updateStudent(
                                        student.copy(
                                            firebaseId = apiStudent.id,
                                            isSynced = true
                                        )
                                    )
                                }
                            } else {
                                // Existing student - update via API
                                studentApiService.updateStudent(
                                    student.firebaseId,
                                    StudentRequest.fromStudent(student)
                                )
                                studentDao.updateStudent(student.copy(isSynced = true))
                            }
                        } catch (e: Exception) {
                            // Individual student sync failed, continue with others
                            e.printStackTrace()
                        }
                    }
                }

                // Pull latest data from Firebase cloud
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

                return Resource.Success(true)
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    // Wait before retry (exponential backoff simplified)
                    kotlinx.coroutines.delay(1000L * attempt)
                }
            }
        }

        return Resource.Error("Sync failed after $maxRetries attempts: ${lastException?.message}", lastException)
    }

    // Force sync from API to local - NEW METHOD
    suspend fun forceSyncFromApi(): Resource<Boolean> {
        return try {
            if (!networkManager.isNetworkAvailable()) {
                return Resource.Error("No network connection available")
            }

            val response = studentApiService.getStudents()
            if (response.isSuccessful && response.body()?.success == true) {
                val apiStudents = response.body()!!.data ?: emptyList()

                // Convert to local students and insert/update in local DB
                apiStudents.forEach { apiStudent ->
                    val localStudent = studentDao.getStudentByFirebaseId(apiStudent.id)
                    if (localStudent == null) {
                        // New student from API
                        studentDao.insertStudent(apiStudent.toStudent())
                    } else {
                        // Update existing student if API has newer version
                        if (apiStudent.updatedAt > localStudent.updatedAt) {
                            studentDao.updateStudent(apiStudent.toStudent())
                        }
                    }
                }
                Resource.Success(true)
            } else {
                Resource.Error("API sync failed: ${response.body()?.error}")
            }
        } catch (e: Exception) {
            Resource.Error("Force sync from API failed: ${e.message}", e)
        }
    }

    // Force sync from cloud to local - ENHANCED WITH BATCH OPERATIONS
    suspend fun forceSyncFromCloud(): Resource<Boolean> {
        return try {
            val cloudStudents = firebaseManager.pullStudentsFromCloud()

            // Use transaction for batch operations
            studentDao.deleteAllStudents()

            cloudStudents.forEach { student ->
                studentDao.insertStudent(student.copy(isSynced = true))
            }

            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error("Force sync failed: ${e.message}", e)
        }
    }

    // Search students - ENHANCED WITH API SEARCH
    suspend fun searchStudents(query: String, fuzzySearch: Boolean = false): Resource<List<Student>> {
        return try {
            var results = emptyList<Student>()

            // First try local search
            val allStudents = studentDao.getAllStudents().first()
            results = if (fuzzySearch && query.length > 2) {
                // Simple fuzzy search implementation
                allStudents.filter { student ->
                    listOf(
                        student.name,
                        student.regNumber,
                        student.course,
                        student.email
                    ).any { field ->
                        field.contains(query, ignoreCase = true) ||
                                calculateSimilarity(field, query) > 0.7
                    }
                }
            } else {
                // Exact search
                allStudents.filter { student ->
                    student.name.contains(query, ignoreCase = true) ||
                            student.regNumber.contains(query, ignoreCase = true) ||
                            student.course.contains(query, ignoreCase = true) ||
                            student.email.contains(query, ignoreCase = true)
                }
            }

            // If no local results and network available, try API search
            if (results.isEmpty() && networkManager.isNetworkAvailable()) {
                try {
                    val response = studentApiService.searchStudents(query)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val apiStudents = response.body()!!.data ?: emptyList()
                        results = apiStudents.map { it.toStudent() }
                        // Save API results to local database for offline access
                        results.forEach { studentDao.insertStudent(it) }
                    }
                } catch (e: Exception) {
                    // API search failed, use local results (empty)
                    e.printStackTrace()
                }
            }

            Resource.Success(results)
        } catch (e: Exception) {
            Resource.Error("Search failed: ${e.message}", e)
        }
    }

    // ==================== NEW CAPABILITIES WITH RETROFIT ====================

    /**
     * Bulk operations for better performance with large datasets
     */
    suspend fun addStudentsInBatch(students: List<Student>): Resource<Int> {
        return try {
            val studentsWithIds = students.map { student ->
                if (student.id.isEmpty()) {
                    student.copy(id = UUID.randomUUID().toString())
                } else {
                    student
                }
            }

            studentDao.insertStudents(studentsWithIds)

            // Try to sync batch with API if network available
            if (networkManager.isNetworkAvailable()) {
                try {
                    val studentRequests = studentsWithIds.map { StudentRequest.fromStudent(it) }
                    // Note: You might want to create a bulk endpoint in your API
                    studentRequests.forEach { studentRequest ->
                        studentApiService.createStudent(studentRequest)
                    }
                } catch (e: Exception) {
                    // Batch API sync failed, but local insert succeeded
                    e.printStackTrace()
                }
            }

            Resource.Success(studentsWithIds.size)
        } catch (e: Exception) {
            Resource.Error("Batch insert failed: ${e.message}", e)
        }
    }

    /**
     * Get students with pagination support
     */
    suspend fun getStudentsPaginated(limit: Int, offset: Int): Resource<List<Student>> {
        return try {
            // Note: You'll need to add this method to your StudentDao
            val students = studentDao.getStudentsPaginated(limit, offset)
            Resource.Success(students)
        } catch (e: Exception) {
            Resource.Error("Pagination query failed: ${e.message}", e)
        }
    }

    /**
     * Get sync statistics
     */
    suspend fun getSyncStatistics(): Resource<SyncStats> {
        return try {
            val totalStudents = studentDao.getStudentCount()
            val syncedStudents = studentDao.getSyncedStudentCount()

            Resource.Success(
                SyncStats(
                    totalStudents = totalStudents,
                    syncedStudents = syncedStudents,
                    syncPercentage = if (totalStudents > 0) (syncedStudents * 100 / totalStudents) else 100
                )
            )
        } catch (e: Exception) {
            Resource.Error("Failed to get sync stats: ${e.message}", e)
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    // Sync from API - new private method
    private suspend fun syncFromApi() {
        try {
            val response = studentApiService.getStudents()
            if (response.isSuccessful && response.body()?.success == true) {
                val apiStudents = response.body()!!.data ?: emptyList()

                // Convert to local students and insert/update in local DB
                apiStudents.forEach { apiStudent ->
                    val localStudent = studentDao.getStudentByFirebaseId(apiStudent.id)
                    if (localStudent == null) {
                        // New student from API
                        studentDao.insertStudent(apiStudent.toStudent())
                    } else {
                        // Update existing student if API has newer version
                        if (apiStudent.updatedAt > localStudent.updatedAt) {
                            studentDao.updateStudent(apiStudent.toStudent())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handle sync error silently
            e.printStackTrace()
        }
    }

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

    // Simple similarity calculation for fuzzy search
    private fun calculateSimilarity(str1: String, str2: String): Double {
        val longer = if (str1.length > str2.length) str1 else str2
        val shorter = if (str1.length > str2.length) str2 else str1

        if (longer.length == 0) return 1.0

        return (longer.length - editDistance(longer, shorter)) / longer.length.toDouble()
    }

    private fun editDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1)

        for (i in 0..s1.length) {
            var lastValue = i
            for (j in 0..s2.length) {
                if (i == 0) {
                    costs[j] = j
                } else {
                    if (j > 0) {
                        var newValue = costs[j - 1]
                        if (s1[i - 1] != s2[j - 1]) {
                            newValue = minOf(minOf(newValue, lastValue), costs[j]) + 1
                        }
                        costs[j - 1] = lastValue
                        lastValue = newValue
                    }
                }
            }
            if (i > 0) costs[s2.length] = lastValue
        }
        return costs[s2.length]
    }
}

// ==================== DATA CLASSES FOR NEW FEATURES ====================

/**
 * Data class for sync statistics
 */
data class SyncStats(
    val totalStudents: Int,
    val syncedStudents: Int,
    val syncPercentage: Int
)