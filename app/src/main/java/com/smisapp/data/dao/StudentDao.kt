package com.smisapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smisapp.data.entity.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :studentId")
    suspend fun getStudentById(studentId: Long): Student?

    @Query("SELECT * FROM students WHERE reg_number = :regNumber")
    suspend fun getStudentByRegNumber(regNumber: String): Student?

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudent(studentId: Long)
}