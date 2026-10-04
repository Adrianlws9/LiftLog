package com.adrian.liftlog.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.adrian.liftlog.data.BodyWeightEntity
import com.adrian.liftlog.data.WorkoutRepository
import kotlinx.coroutines.launch

class BodyWeightViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkoutRepository(application)

    val allEntries: LiveData<List<BodyWeightEntity>> = repository.getAllBodyWeightEntriesLiveData()

    fun addEntry(weight: Double, unit: String) {
        viewModelScope.launch {
            repository.addBodyWeightEntry(weight, unit)
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteBodyWeightEntry(id)
        }
    }
}