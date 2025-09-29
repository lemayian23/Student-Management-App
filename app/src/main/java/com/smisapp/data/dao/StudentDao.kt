package com.smisapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smisapp.data.entity.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Update
    suspend fun updateStudent(student: Student)

    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :studentId")
    suspend fun getStudentById(studentId: String): Student?

    @Query("SELECT * FROM students WHERE reg_number = :regNumber")
    suspend fun getStudentByRegNumber(regNumber: String): Student?

    @Query("SELECT * FROM students WHERE firebase_id = :firebaseId")
    suspend fun getStudentByFirebaseId(firebaseId: String): Student?

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudent(studentId: String)

    @Query("SELECT * FROM students WHERE is_synced = 0")
    suspend fun getUnsyncedStudents(): List<Student>

    @Query("UPDATE students SET is_synced = 1 WHERE id = :studentId")
    suspend fun markAsSynced(studentId: String)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()
}