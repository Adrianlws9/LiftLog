package com.adrian.liftlog

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.adrian.liftlog.adapter.CustomExerciseAdapter
import com.adrian.liftlog.databinding.ActivityCustomWorkoutBinding
import com.adrian.liftlog.model.DefaultWorkoutPlan
import java.util.Calendar

class CustomWorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomWorkoutBinding
    private val exerciseNames = mutableListOf<String>()
    private lateinit var adapter: CustomExerciseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCustomWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.customWorkoutToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Custom Workout"

        adapter = CustomExerciseAdapter(exerciseNames) { position ->
            exerciseNames.removeAt(position)
            adapter.notifyItemRemoved(position)
        }
        binding.customExercisesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.customExercisesRecyclerView.adapter = adapter

        binding.addCustomExerciseButton.setOnClickListener { showAddExerciseDialog() }

        binding.startCustomWorkoutButton.setOnClickListener { startCustomWorkout() }
    }

    private fun showAddExerciseDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Exercise name"
        AlertDialog.Builder(this)
            .setTitle("Add exercise")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    exerciseNames.add(name)
                    adapter.notifyItemInserted(exerciseNames.size - 1)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startCustomWorkout() {
        val titleText = binding.customTitleInput.text.toString().trim().ifEmpty { "Custom Workout" }

        if (exerciseNames.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("No exercises added")
                .setMessage("Add at least one exercise before starting.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val todaysDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val dayName = DefaultWorkoutPlan.dayNameFor(todaysDayOfWeek)

        val intent = Intent(this, WorkoutActivity::class.java)
        intent.putExtra("day_name", dayName)
        intent.putExtra("workout_title", titleText)
        intent.putExtra("exercise_names", exerciseNames.toTypedArray())
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}