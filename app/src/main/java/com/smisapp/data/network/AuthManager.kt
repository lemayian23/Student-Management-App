package com.smisapp.data.network

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.Executor

class AuthManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private val biometricManager = BiometricManager.from(context)

    // Check if biometric authentication is available
    fun isBiometricAvailable(): Boolean {
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Biometric authentication
    fun authenticateWithBiometric(activity: FragmentActivity) = callbackFlow<Boolean> {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("SMIS Authentication")
            .setSubtitle("Use your biometric credential to login")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    trySend(false).isSuccess
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    trySend(true).isSuccess
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    trySend(false).isSuccess
                }
            })

        biometricPrompt.authenticate(promptInfo)

        awaitClose {
            // Cleanup if needed
        }
    }

    // Session management
    fun saveSession(email: String, password: String?) {
        with(sharedPreferences.edit()) {
            putString("user_email", email)
            password?.let { putString("user_password", it) }
            putBoolean("is_logged_in", true)
            putLong("login_time", System.currentTimeMillis())
            apply()
        }
    }

    fun getSavedEmail(): String? {
        return sharedPreferences.getString("user_email", null)
    }

    fun isSessionValid(): Boolean {
        val loginTime = sharedPreferences.getLong("login_time", 0)
        val currentTime = System.currentTimeMillis()
        val sessionDuration = 24 * 60 * 60 * 1000 // 24 hours

        return sharedPreferences.getBoolean("is_logged_in", false) &&
                (currentTime - loginTime) < sessionDuration
    }

    fun clearSession() {
        with(sharedPreferences.edit()) {
            remove("user_email")
            remove("user_password")
            remove("is_logged_in")
            remove("login_time")
            apply()
        }
    }

    // Token management (for future use with APIs)
    fun saveAuthToken(token: String) {
        sharedPreferences.edit().putString("auth_token", token).apply()
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }
}