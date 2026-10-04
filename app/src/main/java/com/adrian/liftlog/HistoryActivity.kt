package com.adrian.liftlog

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.adrian.liftlog.adapter.HistoryAdapter
import com.adrian.liftlog.databinding.ActivityHistoryBinding
import com.adrian.liftlog.ui.HistoryViewModel

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var viewModel: HistoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.historyToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.allWorkouts.observe(this) { workouts ->
            binding.historyLoadingSpinner.visibility = android.view.View.GONE
            binding.historyContentGroup.visibility = android.view.View.VISIBLE
            binding.emptyHistoryText.visibility =
                if (workouts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

            binding.historyRecyclerView.adapter = HistoryAdapter(
                workouts = workouts,
                onWorkoutClick = { workout ->
                    val intent = Intent(this, WorkoutDetailActivity::class.java)
                    intent.putExtra("workout_id", workout.id)
                    startActivity(intent)
                },
                onDeleteClick = { workout ->
                    AlertDialog.Builder(this)
                        .setTitle("Delete workout?")
                        .setMessage("${workout.title} — this can't be undone.")
                        .setPositiveButton("Delete") { _, _ ->
                            viewModel.deleteWorkout(workout.id)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}