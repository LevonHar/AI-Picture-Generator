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

        @Volatile
        private var instance: SharedPrefManager? = null

        fun getInstance(context: Context): SharedPrefManager {
            return instance ?: synchronized(this) {
                instance ?: SharedPrefManager(context.applicationContext).also { instance = it }
            }
        }
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

    // Clear all data (logout)
    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty() || getUserId() != null
    }
}