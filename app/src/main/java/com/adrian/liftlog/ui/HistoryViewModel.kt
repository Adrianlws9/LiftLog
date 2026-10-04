package com.adrian.liftlog.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.adrian.liftlog.data.WorkoutEntity
import com.adrian.liftlog.data.WorkoutRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkoutRepository(application)

    val allWorkouts: LiveData<List<WorkoutEntity>> = repository.getAllWorkoutsLiveData()

    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch {
            repository.deleteWorkout(workoutId)
        }
    }
}