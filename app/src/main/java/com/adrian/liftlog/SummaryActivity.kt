package com.adrian.liftlog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.adrian.liftlog.databinding.ActivitySummaryBinding
import com.adrian.liftlog.model.formatDuration
import com.adrian.liftlog.ui.SummaryViewModel

class SummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySummaryBinding
    private lateinit var viewModel: SummaryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.summaryToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Monthly Summary"

        viewModel = ViewModelProvider(this)[SummaryViewModel::class.java]

        viewModel.summary.observe(this) { summary ->
            binding.summaryMonthText.text = summary.monthLabel
            binding.workoutCountText.text = summary.workoutCount.toString()
            binding.totalTimeText.text = formatDuration(summary.totalDurationSeconds)
            binding.totalSetsText.text = summary.totalSets.toString()

            val volumeStr = if (summary.totalVolumeKg >= 1000) {
                String.format("%.1fk kg", summary.totalVolumeKg / 1000)
            } else {
                String.format("%.0f kg", summary.totalVolumeKg)
            }
            binding.totalVolumeText.text = volumeStr

            if (summary.mostFrequentExercise != null) {
                binding.mostFrequentCard.visibility = android.view.View.VISIBLE
                binding.mostFrequentExerciseText.text = summary.mostFrequentExercise
            } else {
                binding.mostFrequentCard.visibility = android.view.View.GONE
            }

            if (summary.workoutCountVsLastMonth != null) {
                binding.comparisonText.visibility = android.view.View.VISIBLE
                val delta = summary.workoutCountVsLastMonth
                binding.comparisonText.text = when {
                    delta > 0 -> "+$delta workouts vs last month"
                    delta < 0 -> "$delta workouts vs last month"
                    else -> "Same workout count as last month"
                }
            } else {
                binding.comparisonText.visibility = android.view.View.GONE
            }

            if (summary.bodyWeightChangeKg != null) {
                binding.weightChangeText.visibility = android.view.View.VISIBLE
                val change = summary.bodyWeightChangeKg
                val sign = if (change > 0) "+" else ""
                binding.weightChangeText.text = String.format("%s%.1f kg body weight this month", sign, change)
            } else {
                binding.weightChangeText.visibility = android.view.View.GONE
            }
        }

        viewModel.load()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}