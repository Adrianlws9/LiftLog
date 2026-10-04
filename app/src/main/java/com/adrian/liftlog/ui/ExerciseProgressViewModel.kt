package com.adrian.liftlog.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.adrian.liftlog.data.WorkoutRepository
import kotlinx.coroutines.launch

class ExerciseProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkoutRepository(application)

    private val _exerciseNames = MutableLiveData<List<String>>()
    val exerciseNames: LiveData<List<String>> = _exerciseNames

    private val _progress = MutableLiveData<List<Pair<Long, Double>>>()
    val progress: LiveData<List<Pair<Long, Double>>> = _progress

    fun loadExerciseNames() {
        viewModelScope.launch {
            _exerciseNames.value = repository.getAllExerciseNames()
        }
    }

    fun loadProgress(exerciseName: String) {
        viewModelScope.launch {
            _progress.value = repository.getExerciseProgressInKg(exerciseName)
        }
    }
}