package com.adrian.liftlog.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.adrian.liftlog.data.WorkoutRepository
import com.adrian.liftlog.model.WorkoutHistoryDetail
import kotlinx.coroutines.launch

class WorkoutDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkoutRepository(application)

    private val _workoutDetail = MutableLiveData<WorkoutHistoryDetail?>()
    val workoutDetail: LiveData<WorkoutHistoryDetail?> = _workoutDetail

    fun loadWorkout(workoutId: Long) {
        viewModelScope.launch {
            _workoutDetail.value = repository.getWorkoutDetail(workoutId)
        }
    }
}