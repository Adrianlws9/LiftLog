package com.adrian.liftlog.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.adrian.liftlog.data.PlanDayEntity
import com.adrian.liftlog.data.WorkoutRepository
import kotlinx.coroutines.launch

class PlanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkoutRepository(application)

    private val _days = MutableLiveData<List<PlanDayEntity>>()
    val days: LiveData<List<PlanDayEntity>> = _days

    fun loadDays() {
        viewModelScope.launch {
            _days.value = repository.getAllPlanDays()
        }
    }
}