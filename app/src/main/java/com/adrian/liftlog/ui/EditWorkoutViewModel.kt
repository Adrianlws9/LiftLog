package com.adrian.liftlog.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.adrian.liftlog.data.WorkoutRepository
import com.adrian.liftlog.model.LoggedExercise
import kotlinx.coroutines.launch

class EditWorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkoutRepository(application)

    private val _existingExercises = MutableLiveData<List<LoggedExercise>>()
    val existingExercises: LiveData<List<LoggedExercise>> = _existingExercises

    fun loadWorkout(workoutId: Long) {
        viewModelScope.launch {
            _existingExercises.value = repository.getWorkoutForEditing(workoutId)
        }
    }

    fun saveEdits(
        workoutId: Long,
        title: String,
        notes: String?,
        exercises: List<LoggedExercise>,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateWorkout(workoutId, title, notes, exercises)
            onSaved()
        }
    }
}