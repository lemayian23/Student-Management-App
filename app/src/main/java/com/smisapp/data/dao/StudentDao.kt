package com.smisapp.data.dao

import androidx.room.*
import com.smisapp.data.entity.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    // ==================== EXISTING METHODS ====================

    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Update
    suspend fun updateStudent(student: Student)

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudent(studentId: String)

    @Query("SELECT * FROM students WHERE id = :studentId")
    suspend fun getStudentById(studentId: String): Student?

    @Query("SELECT * FROM students WHERE firebaseId = :firebaseId")
    suspend fun getStudentByFirebaseId(firebaseId: String): Student?

    @Query("SELECT * FROM students WHERE isSynced = 0")
    suspend fun getUnsyncedStudents(): List<Student>

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()

    // ==================== NEW METHODS FOR ENHANCED FEATURES ====================

    /**
     * Batch insert for better performance with multiple students
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    /**
     * Pagination support for large datasets
     */
    @Query("SELECT * FROM students ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getStudentsPaginated(limit: Int, offset: Int): List<Student>

    /**
     * Get total student count for statistics
     */
    @Query("SELECT COUNT(*) FROM students")
    suspend fun getStudentCount(): Int

    /**
     * Get count of synced students for statistics
     */
    @Query("SELECT COUNT(*) FROM students WHERE isSynced = 1")
    suspend fun getSyncedStudentCount(): Int

    /**
     * Search students with multiple criteria (for more advanced search functionality)
     */
    @Query("""
        SELECT * FROM students 
        WHERE name LIKE '%' || :query || '%' 
           OR regNumber LIKE '%' || :query || '%' 
           OR course LIKE '%' || :query || '%' 
           OR email LIKE '%' || :query || '%'
        ORDER BY 
            CASE 
                WHEN name LIKE :query || '%' THEN 1
                WHEN regNumber LIKE :query || '%' THEN 2
                ELSE 3
            END,
            name ASC
    """)
    suspend fun searchStudentsAdvanced(query: String): List<Student>

    /**
     * Get students by course for filtering
     */
    @Query("SELECT * FROM students WHERE course LIKE '%' || :course || '%' ORDER BY name ASC")
    suspend fun getStudentsByCourse(course: String): List<Student>

    /**
     * Get students by sync status for maintenance operations
     */
    @Query("SELECT * FROM students WHERE isSynced = :isSynced ORDER BY name ASC")
    suspend fun getStudentsBySyncStatus(isSynced: Boolean): List<Student>

    /**
     * Bulk update sync status
     */
    @Query("UPDATE students SET isSynced = :isSynced WHERE id IN (:studentIds)")
    suspend fun updateSyncStatus(studentIds: List<String>, isSynced: Boolean)

    /**
     * Get students registered within a date range
     * Note: You'll need to add registrationDate to your Student entity if you want this
     */
    // @Query("SELECT * FROM students WHERE registrationDate BETWEEN :startDate AND :endDate ORDER BY registrationDate DESC")
    // suspend fun getStudentsByRegistrationDate(startDate: Long, endDate: Long): List<Student>

    /**
     * Delete multiple students in batch
     */
    @Query("DELETE FROM students WHERE id IN (:studentIds)")
    suspend fun deleteStudents(studentIds: List<String>)

    /**
     * Get students with missing required fields (for data validation)
     */
    @Query("SELECT * FROM students WHERE name = '' OR regNumber = '' OR course = ''")
    suspend fun getStudentsWithMissingData(): List<Student>

    /**
     * Update multiple students in batch
     */
    @Update
    suspend fun updateStudents(students: List<Student>)
}