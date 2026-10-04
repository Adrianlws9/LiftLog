package com.adrian.liftlog.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adrian.liftlog.data.WorkoutRepository
import com.adrian.liftlog.model.LoggedExercise
import kotlinx.coroutines.launch
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.adrian.liftlog.model.LoggedSet
import com.adrian.liftlog.model.PersonalBest

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkoutRepository(application)

    private val _lastPerformances = MutableLiveData<Map<String, List<LoggedSet>>>()
    val lastPerformances: LiveData<Map<String, List<LoggedSet>>> = _lastPerformances

    fun loadLastPerformances(exerciseNames: List<String>, excludeWorkoutId: Long) {
        viewModelScope.launch {
            _lastPerformances.value = repository.getLastPerformances(exerciseNames, excludeWorkoutId)
        }
    }

    private val _previousNotes = MutableLiveData<String?>()
    val previousNotes: LiveData<String?> = _previousNotes

    private val _personalBests = MutableLiveData<Map<String, PersonalBest>>()
    val personalBests: LiveData<Map<String, PersonalBest>> = _personalBests

    fun loadPersonalBests(exerciseNames: List<String>) {
        viewModelScope.launch {
            _personalBests.value = repository.getPersonalBests(exerciseNames)
        }
    }

    fun loadPreviousNotes(title: String) {
        viewModelScope.launch {
            _previousNotes.value = repository.getMostRecentNotes(title)
        }
    }
    fun saveWorkout(
        dayName: String,
        title: String,
        exercises: List<LoggedExercise>,
        durationSeconds: Long,
        notes: String?,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            repository.saveCompletedWorkout(dayName, title, exercises, durationSeconds, notes)
            onSaved()
        }
    }
}