package com.example.logix.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPrefManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "logix_prefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_EMAIL = "email"

        // Temp user data
        private const val KEY_TEMP_USER_ID = "temp_user_id"
        private const val KEY_TEMP_USER_NAME = "temp_user_name"
        private const val KEY_TEMP_PASSWORD = "temp_password"

        // Remember Me constants
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SAVED_EMAIL = "saved_email"
        private const val KEY_SAVED_PASSWORD = "saved_password"

        @Volatile
        private var instance: SharedPrefManager? = null

        fun getInstance(context: Context): SharedPrefManager {
            return instance ?: synchronized(this) {
                instance ?: SharedPrefManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // Add this method to get SharedPreferences
    fun getSharedPreferences(): SharedPreferences {
        return sharedPreferences
    }

    // Token methods
    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    // User info methods
    fun saveUserInfo(userId: String, userName: String, email: String) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun getUserName(): String? {
        return sharedPreferences.getString(KEY_USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    // Email for verification
    fun saveEmail(email: String) {
        sharedPreferences.edit().putString(KEY_EMAIL, email).apply()
    }

    fun getEmail(): String? {
        return sharedPreferences.getString(KEY_EMAIL, null)
    }

    fun clearEmail() {
        sharedPreferences.edit().remove(KEY_EMAIL).apply()
    }

    // Temp user info methods
    fun saveTempUserInfo(userId: String, userName: String, password: String) {
        sharedPreferences.edit().apply {
            putString(KEY_TEMP_USER_ID, userId)
            putString(KEY_TEMP_USER_NAME, userName)
            putString(KEY_TEMP_PASSWORD, password)
            apply()
        }
    }

    fun getTempUserId(): String? {
        return sharedPreferences.getString(KEY_TEMP_USER_ID, null)
    }

    fun getTempUserName(): String? {
        return sharedPreferences.getString(KEY_TEMP_USER_NAME, null)
    }

    fun getTempPassword(): String? {
        return sharedPreferences.getString(KEY_TEMP_PASSWORD, null)
    }

    fun clearTempUserInfo() {
        sharedPreferences.edit().apply {
            remove(KEY_TEMP_USER_ID)
            remove(KEY_TEMP_USER_NAME)
            remove(KEY_TEMP_PASSWORD)
            apply()
        }
    }

    // ==================== REMEMBER ME METHODS ====================

    /**
     * Save remember me preference and credentials
     * @param remember Boolean indicating if remember me is enabled
     * @param email Email to save (optional)
     * @param password Password to save (optional)
     */
    fun setRememberMe(remember: Boolean, email: String = "", password: String = "") {
        sharedPreferences.edit().apply {
            putBoolean(KEY_REMEMBER_ME, remember)
            if (remember && email.isNotEmpty() && password.isNotEmpty()) {
                putString(KEY_SAVED_EMAIL, email)
                putString(KEY_SAVED_PASSWORD, password)
            } else {
                remove(KEY_SAVED_EMAIL)
                remove(KEY_SAVED_PASSWORD)
            }
            apply()
        }
    }

    /**
     * Check if remember me is enabled
     */
    fun isRememberMeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
    }

    /**
     * Get saved email from remember me
     */
    fun getSavedEmail(): String? {
        return sharedPreferences.getString(KEY_SAVED_EMAIL, null)
    }

    /**
     * Get saved password from remember me
     */
    fun getSavedPassword(): String? {
        return sharedPreferences.getString(KEY_SAVED_PASSWORD, null)
    }

    /**
     * Clear saved credentials (useful when user unchecks remember me)
     */
    fun clearSavedCredentials() {
        sharedPreferences.edit().apply {
            remove(KEY_SAVED_EMAIL)
            remove(KEY_SAVED_PASSWORD)
            remove(KEY_REMEMBER_ME)
            apply()
        }
    }

    // ==================== ENHANCED LOGOUT METHODS ====================

    /**
     * Clear user data but keep remember me credentials if enabled
     * Use this when logging out with remember me checked
     */
    fun clearUserData() {
        sharedPreferences.edit().apply {
            remove(KEY_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_EMAIL)
            // Keep remember me data if needed
            apply()
        }
    }

    /**
     * Complete logout - clears all data
     * Use this when logging out with remember me unchecked
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * Smart logout - handles remember me preference
     * @param keepCredentials If true, keeps remember me credentials; if false, clears everything
     */
    fun logout(keepCredentials: Boolean = false) {
        if (keepCredentials) {
            // Keep remember me data, clear everything else
            val rememberMe = isRememberMeEnabled()
            val savedEmail = getSavedEmail()
            val savedPassword = getSavedPassword()

            sharedPreferences.edit().clear().apply()

            // Restore remember me data if it was enabled
            if (rememberMe && !savedEmail.isNullOrEmpty()) {
                setRememberMe(true, savedEmail, savedPassword ?: "")
            }
        } else {
            // Clear everything
            clearAll()
        }
    }

    /**
     * Check if user has valid login session
     * Enhanced to check both token and user info
     */
    fun isValidSession(): Boolean {
        return getToken() != null && getUserId() != null
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty() || getUserId() != null
    }
}