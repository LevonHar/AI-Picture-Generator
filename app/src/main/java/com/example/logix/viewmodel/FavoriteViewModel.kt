package com.example.logix.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.logix.models.LogoItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavoriteViewModel : ViewModel() {

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    // Call this method from your fragments to initialize SharedPreferences
    fun initialize(sharedPreferences: SharedPreferences) {
        prefs = sharedPreferences
        loadFavorites()
    }

    private val _favoriteLogos = MutableLiveData<MutableList<Int>>(mutableListOf())
    val favoriteLogos: LiveData<MutableList<Int>> get() = _favoriteLogos

    private val _favoriteNetworkLogos = MutableLiveData<MutableList<LogoItem>>(mutableListOf())
    val favoriteNetworkLogos: LiveData<MutableList<LogoItem>> get() = _favoriteNetworkLogos

    private fun loadFavorites() {
        if (!::prefs.isInitialized) return

        val savedInts = prefs.getStringSet("favorite_logos", emptySet()) ?: emptySet()
        _favoriteLogos.value = savedInts.map { it.toInt() }.toMutableList()

        val savedLogosJson = prefs.getString("favorite_network_logos", null)
        if (savedLogosJson != null) {
            val type = object : TypeToken<MutableList<LogoItem>>() {}.type
            _favoriteNetworkLogos.value = gson.fromJson(savedLogosJson, type) ?: mutableListOf()
        } else {
            _favoriteNetworkLogos.value = mutableListOf()
        }
    }

    fun addFavorite(logo: Int) {
        _favoriteLogos.value?.let {
            if (!it.contains(logo)) {
                it.add(logo)
                _favoriteLogos.value = it
                saveFavorites()
            }
        }
    }

    fun addFavorite(logo: LogoItem) {
        _favoriteNetworkLogos.value?.let {
            if (!it.any { existing -> existing.id == logo.id }) {
                it.add(logo)
                _favoriteNetworkLogos.value = it
                saveFavorites()
            }
        }
    }

    fun removeFavorite(logo: Int) {
        _favoriteLogos.value?.let {
            if (it.contains(logo)) {
                it.remove(logo)
                _favoriteLogos.value = it
                saveFavorites()
            }
        }
    }

    fun removeFavorite(logo: LogoItem) {
        _favoriteNetworkLogos.value?.let {
            val removed = it.removeAll { existing -> existing.id == logo.id }
            if (removed) {
                _favoriteNetworkLogos.value = it
                saveFavorites()
            }
        }
    }

    fun isFavorite(logoId: Long): Boolean {
        return _favoriteNetworkLogos.value?.any { it.id == logoId } == true
    }

    fun toggleFavorite(logo: LogoItem, addToFav: Boolean) {
        if (addToFav) {
            addFavorite(logo)
        } else {
            removeFavorite(logo)
        }
    }

    private fun saveFavorites() {
        if (!::prefs.isInitialized) return

        val intSet = _favoriteLogos.value?.map { it.toString() }?.toSet() ?: emptySet()
        prefs.edit().putStringSet("favorite_logos", intSet).apply()

        val logosJson = gson.toJson(_favoriteNetworkLogos.value)
        prefs.edit().putString("favorite_network_logos", logosJson).apply()
    }
}