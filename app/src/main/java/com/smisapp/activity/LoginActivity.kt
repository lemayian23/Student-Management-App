package com.smisapp.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.smisapp.MainActivity
import com.smisapp.R
import com.smisapp.data.network.AuthManager
import com.smisapp.data.network.FirebaseManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnBiometric: Button
    private lateinit var tvSignUp: TextView

    private lateinit var authManager: AuthManager
    private lateinit var firebaseManager: FirebaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeViews()
        setupClickListeners()
        checkExistingSession()
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnBiometric = findViewById(R.id.btnBiometric)
        tvSignUp = findViewById(R.id.tvSignUp)

        authManager = AuthManager(this)
        firebaseManager = FirebaseManager.getInstance()

        // Show/hide biometric button based on availability
        btnBiometric.visibility = if (authManager.isBiometricAvailable()) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }

        // Pre-fill saved email
        authManager.getSavedEmail()?.let { email ->
            etEmail.setText(email)
        }
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            performLogin()
        }

        btnBiometric.setOnClickListener {
            performBiometricLogin()
        }

        tvSignUp.setOnClickListener {
            showSignUpDialog()
        }
    }

    private fun checkExistingSession() {
        if (authManager.isSessionValid()) {
            // Auto-login with saved session
            navigateToMain()
        }
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val success = firebaseManager.signIn(email, password)

            if (success) {
                // Save session
                authManager.saveSession(email, password)
                Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            } else {
                Toast.makeText(this@LoginActivity, "Login failed. Check credentials.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun performBiometricLogin() {
        if (!authManager.isBiometricAvailable()) {
            Toast.makeText(this, "Biometric authentication not available", Toast.LENGTH_SHORT).show()
            return
        }

        authManager.authenticateWithBiometric(this).observe(this) { success ->
            if (success) {
                val savedEmail = authManager.getSavedEmail()
                if (savedEmail != null && authManager.isSessionValid()) {
                    Toast.makeText(this, "Biometric authentication successful!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this, "No valid session found. Please login manually first.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Biometric authentication failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSignUpDialog() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter email first", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Simple sign-up with default password
            val password = "default123" // In real app, ask for password
            val success = firebaseManager.signUp(email, password)

            if (success) {
                Toast.makeText(this@LoginActivity, "Account created! Use password: default123", Toast.LENGTH_LONG).show()
                etPassword.setText("default123")
            } else {
                Toast.makeText(this@LoginActivity, "Sign up failed. Email might be in use.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}