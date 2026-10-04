package com.adrian.liftlog

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.adrian.liftlog.databinding.ActivityWorkoutDetailBinding
import com.adrian.liftlog.databinding.ItemExerciseHistoryBinding
import com.adrian.liftlog.model.ExerciseHistory
import com.adrian.liftlog.model.displayString
import com.adrian.liftlog.ui.WorkoutDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import android.content.Intent
import com.adrian.liftlog.model.formatDuration

class WorkoutDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutDetailBinding
    private lateinit var viewModel: WorkoutDetailViewModel
    private val dateFormat = SimpleDateFormat("MMMM d", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWorkoutDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.detailToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[WorkoutDetailViewModel::class.java]

        val workoutId = intent.getLongExtra("workout_id", -1L)

        viewModel.workoutDetail.observe(this) { detail ->
            if (detail == null) return@observe

            binding.detailDateText.text = dateFormat.format(Date(detail.dateEpochMillis))
            binding.detailTitleText.text = detail.title

            if (detail.durationSeconds != null) {
                binding.detailDurationText.text = formatDuration(detail.durationSeconds)
                binding.detailDurationText.visibility = android.view.View.VISIBLE
            } else {
                binding.detailDurationText.visibility = android.view.View.GONE
            }

            if (!detail.notes.isNullOrBlank()) {
                binding.detailNotesText.text = detail.notes
                binding.detailNotesText.visibility = android.view.View.VISIBLE
            } else {
                binding.detailNotesText.visibility = android.view.View.GONE
            }

            binding.copyWorkoutButton.setOnClickListener {
                val summary = buildWorkoutSummary(detail)
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Workout summary", summary))
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }

            binding.editWorkoutButton.setOnClickListener {
                val intent = Intent(this, WorkoutActivity::class.java)
                intent.putExtra("edit_workout_id", workoutId)
                intent.putExtra("day_name", detail.dayName)
                intent.putExtra("workout_title", detail.title)
                intent.putExtra("existing_notes", detail.notes)
                startActivity(intent)
            }

            binding.detailExercisesContainer.removeAllViews()
            for (exercise in detail.exercises) {
                addExerciseCard(exercise)
            }
        }



        viewModel.loadWorkout(workoutId)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun addExerciseCard(exercise: ExerciseHistory) {
        val cardBinding = ItemExerciseHistoryBinding.inflate(
            LayoutInflater.from(this), binding.detailExercisesContainer, false
        )
        cardBinding.exerciseNameText.text = exercise.exerciseName

        for (set in exercise.sets) {
            val setLine = android.widget.TextView(this).apply {
                text = set.displayString()
                textSize = 16f
                setPadding(0, 4, 0, 4)
                typeface = androidx.core.content.res.ResourcesCompat.getFont(
                    this@WorkoutDetailActivity, R.font.bricolage_grotesque
                )
            }
            cardBinding.setsContainer.addView(setLine)
        }

        binding.detailExercisesContainer.addView(cardBinding.root)
    }

    override fun onResume() {
        super.onResume()
        val workoutId = intent.getLongExtra("workout_id", -1L)
        viewModel.loadWorkout(workoutId)
    }
    private fun buildWorkoutSummary(detail: com.adrian.liftlog.model.WorkoutHistoryDetail): String {
        val sb = StringBuilder()
        sb.append("${detail.dayName} — ${detail.title}\n")
        sb.append("${dateFormat.format(Date(detail.dateEpochMillis))}")
        if (detail.durationSeconds != null) {
            sb.append(" — ${detail.durationSeconds / 60} min")
        }
        sb.append("\n\n")

        for (exercise in detail.exercises) {
            sb.append("${exercise.exerciseName}:\n")
            if (exercise.sets.isEmpty()) {
                sb.append("  (no sets logged)\n")
            } else {
                for ((index, set) in exercise.sets.withIndex()) {
                    sb.append("  Set ${index + 1}: ${set.displayString()}\n")
                }
            }
            sb.append("\n")
        }

        if (!detail.notes.isNullOrBlank()) {
            sb.append("Notes: ${detail.notes}\n")
        }

        return sb.toString().trim()
    }
}