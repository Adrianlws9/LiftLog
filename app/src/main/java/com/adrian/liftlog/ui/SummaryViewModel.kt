package com.adrian.liftlog.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.adrian.liftlog.data.WorkoutRepository
import com.adrian.liftlog.model.MonthlySummary
import kotlinx.coroutines.launch

class SummaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkoutRepository(application)

    private val _summary = MutableLiveData<MonthlySummary>()
    val summary: LiveData<MonthlySummary> = _summary

    fun load(monthOffset: Int = 0) {
        viewModelScope.launch {
            _summary.value = repository.getMonthlySummary(monthOffset)
        }
    }
}