package com.example.logix.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.logix.models.EditedLogoEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EditedLogoRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("edited_logos_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_EDITED_LOGOS = "edited_logos_list"

        @Volatile
        private var instance: EditedLogoRepository? = null

        fun getInstance(context: Context): EditedLogoRepository {
            return instance ?: synchronized(this) {
                instance ?: EditedLogoRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun saveEditedLogo(entry: EditedLogoEntry) {
        val current = getEditedLogos().toMutableList()
        // Replace if already exists (same sourceId + same type)
        current.removeAll { it.sourceId == entry.sourceId && it.isNetworkLogo == entry.isNetworkLogo }
        current.add(0, entry) // newest first
        prefs.edit().putString(KEY_EDITED_LOGOS, gson.toJson(current)).apply()
    }

    fun getEditedLogos(): List<EditedLogoEntry> {
        val json = prefs.getString(KEY_EDITED_LOGOS, null) ?: return emptyList()
        val type = object : TypeToken<List<EditedLogoEntry>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun removeEditedLogo(sourceId: String) {
        val current = getEditedLogos().toMutableList()
        current.removeAll { it.sourceId == sourceId }
        prefs.edit().putString(KEY_EDITED_LOGOS, gson.toJson(current)).apply()
    }

    fun clearAll() {
        prefs.edit().remove(KEY_EDITED_LOGOS).apply()
    }
}