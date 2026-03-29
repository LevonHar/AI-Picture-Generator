package com.example.logix.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.logix.models.EditedLogoEntry
import com.example.logix.utils.EditedLogoRepository

class EditedLogosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EditedLogoRepository.getInstance(application)

    private val _editedLogos = MutableLiveData<List<EditedLogoEntry>>()
    val editedLogos: LiveData<List<EditedLogoEntry>> = _editedLogos

    init {
        loadEditedLogos()
    }

    fun loadEditedLogos() {
        _editedLogos.value = repository.getEditedLogos()
    }

    fun addEditedLogo(entry: EditedLogoEntry) {
        repository.saveEditedLogo(entry)
        loadEditedLogos()
    }

    fun removeEditedLogo(sourceId: String) {
        repository.removeEditedLogo(sourceId)
        loadEditedLogos()
    }
}