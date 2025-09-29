package com.smisapp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.smisapp.data.database.StudentDatabase

class SMISApplication : Application() {

    companion object {
        lateinit var instance: SMISApplication
            private set
    }

    val database: StudentDatabase by lazy {
        StudentDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Firebase manually (since we're not using the plugin)
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Firebase might already be initialized
        }
    }
}