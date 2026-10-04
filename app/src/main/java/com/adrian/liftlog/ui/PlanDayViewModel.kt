package com.adrian.liftlog.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.adrian.liftlog.data.PlanDayEntity
import com.adrian.liftlog.data.PlanExerciseEntity
import com.adrian.liftlog.data.WorkoutRepository
import kotlinx.coroutines.launch

class PlanDayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkoutRepository(application)

    private val _day = MutableLiveData<PlanDayEntity?>()
    val day: LiveData<PlanDayEntity?> = _day

    private val _exercises = MutableLiveData<List<PlanExerciseEntity>>()
    val exercises: LiveData<List<PlanExerciseEntity>> = _exercises

    private var currentDayOfWeek: Int = -1

    fun load(dayOfWeek: Int) {
        currentDayOfWeek = dayOfWeek
        viewModelScope.launch {
            _day.value = repository.getPlanDay(dayOfWeek)
            _exercises.value = repository.getExercisesForDay(dayOfWeek)
        }
    }

    fun saveDayDetails(title: String, isRestDay: Boolean) {
        viewModelScope.launch {
            repository.updatePlanDay(currentDayOfWeek, title, isRestDay)
        }
    }

    fun addExercise(name: String) {
        viewModelScope.launch {
            repository.addPlanExercise(currentDayOfWeek, name)
            _exercises.value = repository.getExercisesForDay(currentDayOfWeek)
        }
    }

    fun renameExercise(exerciseId: Long, name: String) {
        viewModelScope.launch {
            repository.renamePlanExercise(exerciseId, name)
            _exercises.value = repository.getExercisesForDay(currentDayOfWeek)
        }
    }

    fun deleteExercise(exerciseId: Long) {
        viewModelScope.launch {
            repository.deletePlanExercise(exerciseId)
            _exercises.value = repository.getExercisesForDay(currentDayOfWeek)
        }
    }

    fun moveExerciseToDay(exerciseId: Long, newDayOfWeek: Int) {
        viewModelScope.launch {
            repository.movePlanExercise(exerciseId, newDayOfWeek)
            _exercises.value = repository.getExercisesForDay(currentDayOfWeek)
        }
    }

    fun reorder(orderedIds: List<Long>) {
        viewModelScope.launch {
            repository.reorderPlanExercises(currentDayOfWeek, orderedIds)
            _exercises.value = repository.getExercisesForDay(currentDayOfWeek)
        }
    }
}