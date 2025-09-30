package com.smisapp.data.network.api

import com.smisapp.data.network.model.ApiResponse
import com.smisapp.data.network.model.ApiStudent
import com.smisapp.data.network.model.StudentRequest
import retrofit2.Response
import retrofit2.http.*

interface StudentApiService {

    // Get all students
    @GET("students")
    suspend fun getStudents(): Response<ApiResponse<List<ApiStudent>>>

    // Get student by ID
    @GET("students/{id}")
    suspend fun getStudent(@Path("id") id: String): Response<ApiResponse<ApiStudent>>

    // Create new student
    @POST("students")
    suspend fun createStudent(@Body student: StudentRequest): Response<ApiResponse<ApiStudent>>

    // Update student
    @PUT("students/{id}")
    suspend fun updateStudent(
        @Path("id") id: String,
        @Body student: StudentRequest
    ): Response<ApiResponse<ApiStudent>>

    // Delete student
    @DELETE("students/{id}")
    suspend fun deleteStudent(@Path("id") id: String): Response<ApiResponse<Unit>>

    // Search students
    @GET("students/search")
    suspend fun searchStudents(@Query("q") query: String): Response<ApiResponse<List<ApiStudent>>>

    // Bulk sync - for offline-first functionality
    @POST("students/sync")
    suspend fun syncStudents(@Body students: List<StudentRequest>): Response<ApiResponse<SyncResult>>
}

// Sync result model
data class SyncResult(
    val synced: Int,
    val conflicts: Int,
    val errors: List<String>
)