package com.smisapp.data.network

import com.smisapp.data.network.api.StudentApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NetworkManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: NetworkManager? = null

        fun getInstance(): NetworkManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkManager().also { INSTANCE = it }
            }
        }
    }

    // Base URL - you can change this to your actual backend URL
    private val BASE_URL = "https://your-backend-api.com/api/"

    private val studentApiService: StudentApiService by lazy {
        createRetrofit().create(StudentApiService::class.java)
    }

    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Change to NONE for production
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    // Add auth header if needed: .header("Authorization", "Bearer $token")
                    .method(original.method, original.body)

                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .build()
    }

    fun getStudentApiService(): StudentApiService {
        return studentApiService
    }

    // Check network availability (you'll need to implement this)
    fun isNetworkAvailable(): Boolean {
        // You'll need to implement this using ConnectivityManager
        return true // Placeholder
    }
}