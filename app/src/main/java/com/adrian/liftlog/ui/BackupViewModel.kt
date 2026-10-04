package com.adrian.liftlog.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adrian.liftlog.data.WorkoutRepository
import kotlinx.coroutines.launch

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkoutRepository(application)

    fun exportData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportAllDataAsJson()
            onResult(json)
        }
    }

    fun importData(json: String, onResult: (Result<Pair<Int, Int>>) -> Unit) {
        viewModelScope.launch {
            try {
                val counts = repository.importAllDataFromJson(json)
                onResult(Result.success(counts))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }
}