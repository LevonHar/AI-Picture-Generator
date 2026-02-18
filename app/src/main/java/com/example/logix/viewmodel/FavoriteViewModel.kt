package com.example.logix.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class FavoriteViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)

    private val _favoriteLogos = MutableLiveData<MutableList<Int>>(mutableListOf())
    val favoriteLogos: LiveData<MutableList<Int>> get() = _favoriteLogos

    init {
        // Load saved favorites from SharedPreferences
        val saved = prefs.getStringSet("favorite_logos", emptySet()) ?: emptySet()
        _favoriteLogos.value = saved.map { it.toInt() }.toMutableList()
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

    fun removeFavorite(logo: Int) {
        _favoriteLogos.value?.let {
            if (it.contains(logo)) {
                it.remove(logo)
                _favoriteLogos.value = it
                saveFavorites()
            }
        }
    }

    private fun saveFavorites() {
        val set = _favoriteLogos.value?.map { it.toString() }?.toSet() ?: emptySet()
        prefs.edit().putStringSet("favorite_logos", set).apply()
    }
}
